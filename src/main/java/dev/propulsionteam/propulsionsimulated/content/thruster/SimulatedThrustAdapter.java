package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CreatePropulsion.ID)
public final class SimulatedThrustAdapter {
    private static final Map<ServerSubLevel, Vector3d> PROPULSION_IMPULSES = new HashMap<>();
    private static final ResourceLocation SPACE_DIMENSION = ResourceLocation.parse("astronautics:space");
    private static final ResourceLocation SPACE_THRUST_GROUP = ResourceLocation.parse("astronautics:thrusters");

    private SimulatedThrustAdapter() {
    }

    public static void applyImpulseAtPoint(final ServerSubLevel subLevel, final Vector3d pointLocal, final Vector3d impulseLocal) {
        ForceGroup group = ForceGroups.PROPULSION.get();
        if (subLevel.getLevel().dimension().location().equals(SPACE_DIMENSION)) {
            group = ForceGroups.REGISTRY.getOptional(SPACE_THRUST_GROUP).orElse(group);
        }
        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(group);
        forceGroup.applyAndRecordPointForce(pointLocal, impulseLocal);
    }

    public static void applyImpulseAtPoint(final ServerSubLevel subLevel, final Vector3d pointLocal,
                                           final Vector3d impulseLocal, final BlockEntity thruster) {
        final double maxSpeed = PropulsionConfig.MAX_PROPULSION_SPEED.get();
        if (maxSpeed > 0) {
            final RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
            if (handle != null && handle.isValid()) {
                final Vector3d velocity = handle.getLinearVelocity(new Vector3d());
                final Vector3d impulseWorld = subLevel.logicalPose().transformNormal(new Vector3d(impulseLocal));
                if (velocity.length() >= maxSpeed && velocity.dot(impulseWorld) > 0) return;
            }
        }
        Vector3d applicationPoint = pointLocal;
        if (PropulsionCoreBehaviour.isActiveCached(thruster)) {
            var centerOfMass = subLevel.getMassTracker().getCenterOfMass();
            if (centerOfMass != null) applicationPoint = new Vector3d(centerOfMass);
        }
        applyImpulseAtPoint(subLevel, applicationPoint, impulseLocal);
        if (PropulsionConfig.PROPULSION_DRAG.get() > 0) {
            PROPULSION_IMPULSES.computeIfAbsent(subLevel, ignored -> new Vector3d()).add(impulseLocal);
        }
    }

    @SubscribeEvent
    public static void onPrePhysicsTick(final ForgeSablePrePhysicsTickEvent event) {
        final Level level = event.getPhysicsSystem().getLevel();
        final var iterator = PROPULSION_IMPULSES.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final ServerSubLevel subLevel = entry.getKey();
            if (subLevel.isRemoved()) {
                iterator.remove();
            } else if (subLevel.getLevel() == level) {
                applyPropulsionDrag(subLevel, entry.getValue(), event.getTimeStep());
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        PROPULSION_IMPULSES.clear();
    }

    private static void applyPropulsionDrag(final ServerSubLevel subLevel, final Vector3d thrustImpulse,
                                            final double timeStep) {
        if (subLevel.isRemoved()) return;
        final RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) return;
        final Vector3d velocity = subLevel.logicalPose().transformNormalInverse(handle.getLinearVelocity(new Vector3d()));
        final double speed = velocity.length();
        if (speed <= 0) return;
        final Vector3d direction = velocity.div(speed);
        final double drag = Math.min(PropulsionConfig.PROPULSION_DRAG.get() * speed * timeStep, thrustImpulse.dot(direction));
        if (drag > 0) applyGroundFriction(subLevel, direction.mul(-drag));
    }

    public static void applyGroundFriction(final ServerSubLevel subLevel, final Vector3d groundFrictionImpulse) {
        final QueuedForceGroup dragGroup = subLevel.getOrCreateQueuedForceGroup(ForceGroups.DRAG.get());
        dragGroup.getForceTotal().applyLinearImpulse(groundFrictionImpulse);
    }

    public static @Nullable ServerSubLevel resolveServerSubLevel(final Level level, final BlockPos pos) {
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            return serverSubLevel;
        }
        return null;
    }

    public static Projection projectToWorld(final Level level, final BlockPos pos, final Vector3d localPosition, final Vector3d localDirection) {
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel == null) {
            return new Projection(level, new Vec3(localPosition.x, localPosition.y, localPosition.z), new Vec3(localDirection.x, localDirection.y, localDirection.z));
        }

        final Vector3d worldPos = new Vector3d(localPosition);
        final Vector3d worldDir = new Vector3d(localDirection);
        subLevel.logicalPose().transformPosition(worldPos);
        subLevel.logicalPose().transformNormal(worldDir);

        return new Projection(subLevel.getLevel(), new Vec3(worldPos.x, worldPos.y, worldPos.z), new Vec3(worldDir.x, worldDir.y, worldDir.z));
    }

    public static boolean isOutsideWorldBuildHeight(final Level level, final BlockPos pos) {
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel == null) {
            return level.isOutsideBuildHeight(pos);
        }
        final Vector3d localPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        subLevel.logicalPose().transformPosition(localPos);
        return subLevel.getLevel().isOutsideBuildHeight((int) Math.floor(localPos.y));
    }

    @Nullable
    public static net.minecraft.world.level.block.entity.BlockEntity getBlockEntitySafe(final Level level, final BlockPos pos) {
        if (Sable.HELPER.getContaining(level, pos) != null && isOutsideWorldBuildHeight(level, pos)) {
            return level.getChunkAt(pos).getBlockEntity(pos);
        }
        return level.getBlockEntity(pos);
    }

    public static net.minecraft.world.level.block.state.BlockState getBlockStateSafe(final Level level, final BlockPos pos) {
        if (Sable.HELPER.getContaining(level, pos) != null && isOutsideWorldBuildHeight(level, pos)) {
            return level.getChunkAt(pos).getBlockState(pos);
        }
        return level.getBlockState(pos);
    }

    public record Projection(Level level, Vec3 position, Vec3 direction) {
    }
}
