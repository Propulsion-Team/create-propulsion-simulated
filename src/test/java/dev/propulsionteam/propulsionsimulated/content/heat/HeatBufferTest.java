package dev.propulsionteam.propulsionsimulated.content.heat;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatBufferTest {
    @Test
    void missingSchematicDataPreservesCapacityAndAllowsHeating() {
        for (float capacity : new float[]{400, 800}) {
            HeatBuffer buffer = new HeatBuffer(0, capacity, 1);
            buffer.deserializeNBT(null, new CompoundTag());
            buffer.generateHeat(10);
            assertEquals(capacity, buffer.getMaxHeatStored());
            assertEquals(10, buffer.getHeatStored());
            assertEquals(1, buffer.getExpectedHeatProduction());
        }
    }

    @Test
    void copiedCapacityCannotOverrideTheBlockCapacity() {
        for (float savedCapacity : new float[]{0, -1, 1, 10_000, Float.NaN, Float.POSITIVE_INFINITY}) {
            HeatBuffer buffer = new HeatBuffer(0, 400, 1);
            CompoundTag tag = new CompoundTag();
            tag.putFloat("Capacity", savedCapacity);
            tag.putFloat("Heat", 100);
            buffer.deserializeNBT(null, tag);
            assertEquals(400, buffer.getMaxHeatStored());
            assertEquals(100, buffer.getHeatStored());
        }
    }

    @Test
    void invalidStoredHeatIsClampedOrCleared() {
        for (float savedHeat : new float[]{-10, Float.NaN, Float.POSITIVE_INFINITY}) {
            HeatBuffer buffer = new HeatBuffer(0, 400, 1);
            CompoundTag tag = new CompoundTag();
            tag.putFloat("Heat", savedHeat);
            buffer.deserializeNBT(null, tag);
            assertEquals(0, buffer.getHeatStored());
        }
        HeatBuffer buffer = new HeatBuffer(0, 400, 1);
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Heat", 500);
        buffer.deserializeNBT(null, tag);
        assertEquals(400, buffer.getHeatStored());
    }

    @Test
    void movementAndReloadPreserveStoredHeat() {
        HeatBuffer original = new HeatBuffer(125, 400, 1);
        HeatBuffer moved = new HeatBuffer(0, 400, 1);
        moved.deserializeNBT(null, original.serializeNBT(null));
        moved.generateHeat(1);
        assertEquals(126, moved.getHeatStored());
        assertEquals(400, moved.getMaxHeatStored());
    }
}
