package dev.propulsionteam.propulsionsimulated.content.heat;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

public class HeatBuffer implements IHeatSource, INBTSerializable<CompoundTag> {
    protected float heat;
    protected final float capacity;
    protected float expectedHeatProduction;

    public HeatBuffer(float initialHeat, float capacity, float expectedHeatProduction) {
        this.heat = initialHeat;
        this.capacity = capacity;
        this.expectedHeatProduction = expectedHeatProduction;
    }

    @Override
    public float extractHeat(float amount, boolean simulate) {
        float heatToExtract = Math.min(heat, amount);
        if (!simulate) {
            heat -= heatToExtract;
        }
        return heatToExtract;
    }

    @Override
    public float getHeatStored() {
        return heat;
    }

    @Override
    public float getMaxHeatStored() {
        return capacity;
    }

    @Override
    public float getExpectedHeatProduction() {
        return expectedHeatProduction;
    }

    @Override
    public void generateHeat(float amount) {
        heat = Math.min(capacity, heat + amount);
    }

    @Override
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Heat", this.heat);
        tag.putFloat("Capacity", this.capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider registries, CompoundTag nbt) {
        float savedHeat = nbt.getFloat("Heat");
        this.heat = Float.isFinite(savedHeat) ? Math.max(0, Math.min(capacity, savedHeat)) : 0;
    }
}
