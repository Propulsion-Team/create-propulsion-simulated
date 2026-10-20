package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

final class TiltAdapterModelRotation {
    private TiltAdapterModelRotation() {}

    static Vec3 apply(Vec3 point, Direction facing, boolean alongFirst) {
        int xRotation = switch (facing) {
            case UP -> 90;
            case DOWN -> -90;
            default -> 0;
        };
        int yRotation = switch (facing) {
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            case UP, DOWN -> alongFirst ? -90 : 0;
            default -> 0;
        };
        return VecHelper.rotateCentered(VecHelper.rotateCentered(point, xRotation, Direction.Axis.X), yRotation, Direction.Axis.Y);
    }
}
