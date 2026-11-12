package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class SimulatedThrustAdapter {
    private static final ResourceLocation SPACE_DIMENSION = ResourceLocation.parse("astronautics:space");
    private static final ResourceLocation SPACE_THRUST_GROUP = ResourceLocation.parse("astronautics:thrusters");

    private SimulatedThrustAdapter() {
    }

    @SuppressWarnings("unchecked")
    private static <T> T getForceGroup(String name) {
        try {
            Field field = ForceGroups.class.getField(name);
            Object ro = field.get(null);
            if (ro instanceof Supplier) {
                return ((Supplier<T>) ro).get();
            }
            Method getMethod = ro.getClass().getMethod("get");
            return (T) getMethod.invoke(ro);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access Sable ForceGroup: " + name, e);
        }
    }

    public static void applyImpulseAtPoint(final ServerSubLevel subLevel, final Vector3d pointLocal, final Vector3d impulseLocal) {
        ForceGroup group = getForceGroup("PROPULSION");
        if (subLevel.getLevel().dimension().location().equals(SPACE_DIMENSION)) {
            group = ForceGroups.REGISTRY.getOptional(SPACE_THRUST_GROUP).orElse(group);
        }
        final QueuedForceGroup forceGroup = subLevel.getOrCreateQueuedForceGroup(group);
        forceGroup.applyAndRecordPointForce(pointLocal, impulseLocal);
    }

    public static void applyGroundFriction(final ServerSubLevel subLevel, final Vector3d groundFrictionImpulse) {
        final QueuedForceGroup dragGroup = subLevel.getOrCreateQueuedForceGroup(getForceGroup("DRAG"));
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
