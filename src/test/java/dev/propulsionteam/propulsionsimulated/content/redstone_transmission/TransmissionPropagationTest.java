package dev.propulsionteam.propulsionsimulated.content.redstone_transmission;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransmissionPropagationTest {
    @Test
    void transmissionUsesCreatesDestinationAwarePropagation() throws NoSuchMethodException {
        var propagation = RedstoneTransmissionBlockEntity.class.getMethod("propagateRotationTo",
            KineticBlockEntity.class, BlockState.class, BlockState.class, BlockPos.class,
            boolean.class, boolean.class);
        assertEquals(KineticBlockEntity.class, propagation.getDeclaringClass());
    }

    @Test
    void preservesAllConfiguredOutputRatios() {
        for (int shift = 0; shift <= RedstoneTransmissionBlockEntity.MAX_VALUE; shift++) {
            assertEquals(shift / 256.0f, RedstoneTransmissionBlockEntity.getOutputSpeedModifier(shift));
        }
        assertEquals(0, RedstoneTransmissionBlockEntity.getOutputSpeedModifier(-1));
        assertEquals(1, RedstoneTransmissionBlockEntity.getOutputSpeedModifier(257));
    }

    @Test
    void adjacentOutputModifiersProduceReciprocalSpeeds() {
        for (int firstShift = 1; firstShift <= 256; firstShift++) {
            for (int secondShift = 1; secondShift <= 256; secondShift++) {
                float first = RedstoneTransmissionBlockEntity.getOutputSpeedModifier(firstShift);
                float second = RedstoneTransmissionBlockEntity.getOutputSpeedModifier(secondShift);
                float forward = first / second;
                float backward = second / first;
                assertEquals(1.0f, forward * backward, 1.0e-6f);
            }
        }
    }
}
