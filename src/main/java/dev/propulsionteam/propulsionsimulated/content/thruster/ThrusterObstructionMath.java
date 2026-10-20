package dev.propulsionteam.propulsionsimulated.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntBinaryOperator;

public final class ThrusterObstructionMath {
    private ThrusterObstructionMath() {}

    public static Vec3 nozzlePosition(BlockPos origin, int width, Direction exhaust, int column, int row) {
        double along = exhaust.getAxisDirection() == Direction.AxisDirection.POSITIVE ? width + 0.45 : -0.45;
        double first = column + 0.5;
        double second = row + 0.5;
        return switch (exhaust.getAxis()) {
            case X -> new Vec3(origin.getX() + along, origin.getY() + first, origin.getZ() + second);
            case Y -> new Vec3(origin.getX() + first, origin.getY() + along, origin.getZ() + second);
            case Z -> new Vec3(origin.getX() + first, origin.getY() + second, origin.getZ() + along);
        };
    }

    public static float efficiency(int width, int scanLength, IntBinaryOperator sample) {
        if (width < 1 || scanLength < 1) return 0;
        double clearance = 0;
        int totalWeight = 0;
        for (int column = 0; column < width; column++) {
            for (int row = 0; row < width; row++) {
                int weight = width == 3 && column == 1 && row == 1 ? 2 : 1;
                clearance += weight * Math.clamp(sample.applyAsInt(column, row), 0, scanLength);
                totalWeight += weight;
            }
        }
        return (float) (clearance / (scanLength * (double) totalWeight));
    }

    public static int emptyBlocks(float efficiency, int scanLength) {
        if (efficiency <= 0 || scanLength < 1) return 0;
        return Math.clamp((int) Math.floor(efficiency * scanLength), 1, scanLength);
    }
}
