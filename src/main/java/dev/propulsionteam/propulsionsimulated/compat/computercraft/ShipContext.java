package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.IComputerSystem;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

final class ShipContext {
    final ServerSubLevel subLevel;
    final Pose3dc pose;
    final Quaterniondc orientation;
    @Nullable
    final MassData mass;
    final Vector3dc centerOfMassPlot;

    private ShipContext(ServerSubLevel subLevel) {
        this.subLevel = subLevel;
        this.pose = subLevel.logicalPose();
        this.orientation = new Quaterniond(pose.orientation()).normalize();
        MassData mass = subLevel.getMassTracker();
        this.mass = mass != null && !mass.isInvalid() ? mass : null;
        this.centerOfMassPlot = new Vector3d(this.mass != null ? this.mass.getCenterOfMass() : pose.rotationPoint());
    }

    @Nullable
    static ShipContext of(IComputerSystem computer) {
        ServerLevel level = computer.getLevel();
        BlockPos pos = computer.getPosition();
        if (level == null || pos == null) return null;
        return Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel ship && !ship.isRemoved()
            ? new ShipContext(ship) : null;
    }

    Vector3d rotateToWorld(Vector3dc local) {
        return orientation.transform(new Vector3d(local));
    }

    Vector3d rotateToLocal(Vector3dc world) {
        return orientation.transformInverse(new Vector3d(world));
    }

    Vector3d plotToLocal(Vector3dc plot) {
        return new Vector3d(plot).sub(centerOfMassPlot);
    }

    Vector3d plotToWorld(Vector3dc plot) {
        return pose.transformPosition(plot, new Vector3d());
    }

    Vector3d centerOfMassWorld() {
        return plotToWorld(centerOfMassPlot);
    }

    double massOrZero() {
        return mass != null ? mass.getMass() : 0.0;
    }

    /** Heading (compass, 0..360), pitch (nose up +) and roll (right side down +), in degrees. */
    double[] headingPitchRoll() {
        Vector3d e = orientation.getEulerAnglesYXZ(new Vector3d());
        return new double[] {
            Mth.positiveModulo(-Math.toDegrees(e.y), 360.0) + 0.0,
            Math.toDegrees(e.x) + 0.0,
            Mth.wrapDegrees(-Math.toDegrees(e.z)) + 0.0
        };
    }

    Vector3d velocityWorld() {
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) return new Vector3d();
        Vector3d arm = centerOfMassWorld().sub(pose.position());
        return handle.getLinearVelocity(new Vector3d()).add(handle.getAngularVelocity(new Vector3d()).cross(arm));
    }

    Vector3d angularVelocityWorldDegrees() {
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        if (handle == null || !handle.isValid()) return new Vector3d();
        return handle.getAngularVelocity(new Vector3d()).mul(180.0 / Math.PI);
    }

    Vector3d airVelocityWorld() {
        return SubLevelHelper.getVelocityRelativeToAir(subLevel.getLevel(), centerOfMassPlot, new Vector3d());
    }

    Vector3d gravityWorld() {
        return DimensionPhysicsData.getGravity(subLevel.getLevel(), centerOfMassWorld(), new Vector3d());
    }
}
