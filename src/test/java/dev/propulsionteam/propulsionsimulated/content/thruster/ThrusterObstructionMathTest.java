package dev.propulsionteam.propulsionsimulated.content.thruster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrusterObstructionMathTest {
    @Test
    void twoByTwoCoverageIsLinearForEveryBlockingPattern() {
        for (int scanLength : new int[]{1, 10, 64}) {
            for (int mask = 0; mask < 16; mask++) {
                int blocked = mask;
                float efficiency = ThrusterObstructionMath.efficiency(2, scanLength,
                    (column, row) -> (blocked & (1 << (column * 2 + row))) == 0 ? scanLength : 0);
                assertEquals(1 - Integer.bitCount(mask) / 4f, efficiency, 1e-6);
            }
        }
    }

    @Test
    void threeByThreeCenterCountsTwiceForEveryBlockingPattern() {
        for (int scanLength : new int[]{1, 10, 64}) {
            for (int mask = 0; mask < 512; mask++) {
                int blocked = mask;
                float efficiency = ThrusterObstructionMath.efficiency(3, scanLength,
                    (column, row) -> (blocked & (1 << (column * 3 + row))) == 0 ? scanLength : 0);
                int blockedWeight = Integer.bitCount(mask) + ((mask & (1 << 4)) == 0 ? 0 : 1);
                assertEquals(1 - blockedWeight / 10f, efficiency, 1e-6);
            }
        }
    }

    @Test
    void partialClearanceRetainsDistanceBasedEfficiency() {
        assertEquals(0.5f, ThrusterObstructionMath.efficiency(2, 10, (column, row) -> 5), 1e-6);
        assertEquals(0.9f, ThrusterObstructionMath.efficiency(3, 10,
            (column, row) -> column == 1 && row == 1 ? 5 : 10), 1e-6);
        assertEquals(0.95f, ThrusterObstructionMath.efficiency(3, 10,
            (column, row) -> column == 0 && row == 0 ? 5 : 10), 1e-6);
    }

    @Test
    void raysCoverEveryExhaustCellInAllSixDirections() {
        BlockPos origin = new BlockPos(10, -50, 30);
        for (int width : new int[]{2, 3}) {
            for (Direction exhaust : Direction.values()) {
                var cells = new HashSet<BlockPos>();
                for (int column = 0; column < width; column++) {
                    for (int row = 0; row < width; row++) {
                        var nozzle = ThrusterObstructionMath.nozzlePosition(origin, width, exhaust, column, row);
                        cells.add(BlockPos.containing(nozzle));
                        for (Direction.Axis axis : Direction.Axis.values()) {
                            double offset = axis.choose(nozzle.x, nozzle.y, nozzle.z)
                                - axis.choose(origin.getX(), origin.getY(), origin.getZ());
                            if (axis == exhaust.getAxis()) {
                                assertEquals(exhaust.getAxisDirection() == Direction.AxisDirection.POSITIVE
                                    ? width + 0.45 : -0.45, offset, 1e-6);
                            } else {
                                assertTrue(offset >= 0.5 && offset <= width - 0.5);
                                assertEquals(0.5, offset - Math.floor(offset), 1e-6);
                            }
                        }
                    }
                }
                assertEquals(width * width, cells.size());
            }
        }
    }

    @Test
    void partialObstructionNeverRoundsToFullyOpenOrFullyBlocked() {
        assertEquals(0, ThrusterObstructionMath.emptyBlocks(0, 10));
        assertEquals(1, ThrusterObstructionMath.emptyBlocks(0.01f, 10));
        assertEquals(9, ThrusterObstructionMath.emptyBlocks(0.95f, 10));
        assertEquals(10, ThrusterObstructionMath.emptyBlocks(1, 10));
        assertEquals(1, ThrusterObstructionMath.emptyBlocks(0.25f, 1));
    }

    @Test
    void invalidClearanceIsClamped() {
        assertEquals(0, ThrusterObstructionMath.efficiency(2, 10, (column, row) -> -10));
        assertEquals(1, ThrusterObstructionMath.efficiency(3, 10, (column, row) -> 100));
        assertEquals(0, ThrusterObstructionMath.efficiency(0, 10, (column, row) -> 10));
        assertEquals(0, ThrusterObstructionMath.efficiency(2, 0, (column, row) -> 10));
    }
}
