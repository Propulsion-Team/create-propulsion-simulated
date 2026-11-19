package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class VectorThrusterPhysics {
    private static final double PIVOT_RIGHT_OFFSET = -1.0d / 16.0d;
    private static final double PIVOT_UP_OFFSET = 1.0d / 16.0d;
    private static final double PIVOT_FORWARD_OFFSET = 7.0d / 16.0d;
    private static final double NOZZLE_OFFSET = 0.45d;

    private VectorThrusterPhysics() {}

    public static Vector3d applicationPoint(BlockPos pos, Direction facing, Vector3dc thrustDirection) {
        Vector3d forward = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        boolean vertical = Math.abs(forward.y) > 0.999d;
        Vector3d right = new Vector3d(forward).cross(vertical ? new Vector3d(0, 0, 1) : new Vector3d(0, 1, 0)).normalize();
        if (vertical) right.negate();
        Vector3d up = new Vector3d(right).cross(forward).normalize();
        Vector3d center = new Vector3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d);
        Vector3d pivot = new Vector3d(center)
                .fma(PIVOT_RIGHT_OFFSET, right)
                .fma(PIVOT_UP_OFFSET, up)
                .fma(PIVOT_FORWARD_OFFSET, forward);
        Vector3d exhaust = new Vector3d(thrustDirection).negate().normalize();
        double yaw = Math.atan2(exhaust.dot(right), -exhaust.dot(forward));
        double pitch = -Math.asin(Math.clamp(exhaust.dot(up), -1.0d, 1.0d));
        return new Vector3d(center).fma(-NOZZLE_OFFSET, forward).sub(pivot)
                .rotateAxis(pitch, right.x, right.y, right.z)
                .rotateAxis(yaw, up.x, up.y, up.z)
                .add(pivot);
    }
}
