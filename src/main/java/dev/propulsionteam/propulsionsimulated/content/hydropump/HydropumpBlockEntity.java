package dev.propulsionteam.propulsionsimulated.content.hydropump;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

public class HydropumpBlockEntity extends PumpBlockEntity {
    private HydropumpSpeedBehaviour pumpingSpeed;
    private final PumpEnergyStorage energy = new PumpEnergyStorage();
    private class PumpEnergyStorage extends EnergyStorage {
        private PumpEnergyStorage() {
            super(PropulsionConfig.HYDROPUMP_ENERGY_CAPACITY_FE.get(),
                PropulsionConfig.HYDROPUMP_ENERGY_CAPACITY_FE.get(), 0);
        }

        private void updateCapacity() {
            capacity = PropulsionConfig.HYDROPUMP_ENERGY_CAPACITY_FE.get();
            maxReceive = capacity;
            energy = Math.min(energy, capacity);
        }

        private void consume(int amount) {
            energy -= amount;
            setChanged();
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            updateCapacity();
            int received = super.receiveEnergy(amount, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }
    }
    private float previousSpeed;
    private long lastBudgetTick = Long.MIN_VALUE;
    private int transferCredit;
    private long energyCredit;

    public HydropumpBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.HYDROPUMP_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        pumpingSpeed = new HydropumpSpeedBehaviour(this);
        behaviours.add(pumpingSpeed);
    }

    public int getPumpingSpeed() {
        int max = HydropumpSpeedBehaviour.maxSpeed();
        return pumpingSpeed == null ? max : Math.clamp(pumpingSpeed.getValue(), 1, max);
    }

    private int getEnergyCost() {
        return 1 + Math.round((getPumpingSpeed() - 1)
            * (PropulsionConfig.HYDROPUMP_FE_PER_TICK_AT_MAX_SPEED.get() - 1f)
            / (HydropumpSpeedBehaviour.maxSpeed() - 1));
    }

    public int getTransferAllowance() {
        long tick = level.getGameTime();
        if (lastBudgetTick != tick) {
            long elapsed = lastBudgetTick == Long.MIN_VALUE ? 1 : tick - lastBudgetTick;
            int rate = getPumpingSpeed();
            transferCredit = (int) Math.min(Math.max(20, rate + 19), transferCredit + elapsed * rate);
            lastBudgetTick = tick;
        }
        long affordable = ((long) energy.getEnergyStored() * getPumpingSpeed() + energyCredit) / (20L * getEnergyCost());
        return (int) Math.min(transferCredit / 20, affordable);
    }

    public void resetTransferBudget() {
        transferCredit = 0;
        energyCredit = 0;
        lastBudgetTick = Long.MIN_VALUE;
    }

    public IEnergyStorage getEnergyHandler(Direction side) {
        return side == null || side.getAxis() != getBlockState().getValue(HydropumpBlock.FACING).getAxis()
            ? energy : null;
    }

    public boolean canTransport() {
        return energy.getEnergyStored() > 0 || energyCredit >= 20 * getEnergyCost();
    }

    public void onFluidTransferred(int amount) {
        transferCredit -= amount * 20;
        int rate = getPumpingSpeed();
        long required = amount * 20L * getEnergyCost() - energyCredit;
        int cost = (int) Math.max(0, (required + rate - 1) / rate);
        energyCredit = (long) cost * rate - required;
        energy.consume(cost);
    }

    @Override
    public float getSpeed() {
        return canTransport() ? Math.max(AllConfigs.server().kinetics.maxRotationSpeed.get(),
            2 * ((getPumpingSpeed() + 19) / 20)) : 0;
    }

    @Override
    public boolean needsSpeedUpdate() {
        return false;
    }

    @Override
    public void tick() {
        if (!level.isClientSide) {
            energy.updateCapacity();
            getTransferAllowance();
            float currentSpeed = getSpeed();
            if (currentSpeed != previousSpeed) {
                previousSpeed = currentSpeed;
                updatePressureChange();
            }
            if (level.getGameTime() % 10 == 0) {
                BlockState state = getBlockState();
                BlockState updated = HydropumpBlock.updateConnections(state, level, worldPosition);
                if (updated != state) {
                    level.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS);
                }
            }
        }
        super.tick();
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return false;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return false;
    }

    private static void removeKineticData(CompoundTag tag) {
        tag.remove("Speed");
        tag.remove("NeedsSpeedUpdate");
        tag.remove("Source");
        tag.remove("Network");
        tag.remove("Sequence");
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        removeKineticData(tag);
        tag.put("Energy", energy.serializeNBT(registries));
        tag.putLong("EnergyCredit", energyCredit);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag = tag.copy();
        removeKineticData(tag);
        super.read(tag, registries, clientPacket);
        if (tag.contains("Energy")) {
            energy.deserializeNBT(registries, tag.get("Energy"));
            energy.updateCapacity();
        }
        energyCredit = Math.clamp(tag.getLong("EnergyCredit"), 0L, getPumpingSpeed() - 1L);
    }
}
