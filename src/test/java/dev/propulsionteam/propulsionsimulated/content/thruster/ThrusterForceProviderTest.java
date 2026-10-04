package dev.propulsionteam.propulsionsimulated.content.thruster;

import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ThrusterForceProviderTest {
    @Test
    void standaloneAndMultiblockControllersUseTheirOwnCenter() {
        BlockPos origin = new BlockPos(10, 20, 30);
        for (int width : new int[]{1, 2, 3}) {
            var sample = ThrusterForceProvider.createSample(origin, null, width,
                new Vector3d(0, 0, 2), 100, 0.05, 0.25);
            assertNotNull(sample);
            assertEquals(new Vector3d(10 + width / 2.0, 20 + width / 2.0, 30 + width / 2.0 + 0.25),
                sample.pointLocal());
            assertEquals(new Vector3d(0, 0, 5), sample.impulseLocal());
        }
    }

    @Test
    void membersDoNotApplyStaleThrustDuringAssemblyOrDisassembly() {
        for (int width : new int[]{1, 2, 3}) {
            assertNull(ThrusterForceProvider.createSample(new BlockPos(1, 0, 0), BlockPos.ZERO, width,
                new Vector3d(0, 0, 1), 100, 0.05, 0.25));
        }
    }

    @Test
    void invalidTopologyAndForceDataAreSkipped() {
        assertNull(ThrusterForceProvider.createSample(BlockPos.ZERO, null, 0,
            new Vector3d(0, 0, 1), 100, 0.05, 0));
        assertNull(ThrusterForceProvider.createSample(BlockPos.ZERO, null, 1,
            new Vector3d(), 100, 0.05, 0));
        for (double thrust : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertNull(ThrusterForceProvider.createSample(BlockPos.ZERO, null, 1,
                new Vector3d(0, 0, 1), thrust, 0.05, 0));
        }
        for (double timeStep : new double[]{0, -1, Double.NaN}) {
            assertNull(ThrusterForceProvider.createSample(BlockPos.ZERO, null, 1,
                new Vector3d(0, 0, 1), 100, timeStep, 0));
        }
    }
}
