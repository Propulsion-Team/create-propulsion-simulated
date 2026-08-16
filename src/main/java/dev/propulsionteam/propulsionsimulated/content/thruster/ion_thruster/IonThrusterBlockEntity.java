package dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster;

import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterDamager;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerBehaviour;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.utility.GoggleUtils;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Locale;

public class IonThrusterBlockEntity extends ThrusterBlockEntity {
    private int energyStored;
    private double energyDrainAccumulator;
    private double lastConsumedFePerTick;

    private final IEnergyStorage energyHandler = new IEnergyStorage() {
        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            final boolean wasEmpty = getTotalEnergyStoredFe() <= 0;
            final int accepted = insertEnergy(maxReceive, simulate);
            if (!simulate && accepted > 0) {
                setChanged();
                // Recalculate immediately only when transitioning from unpowered to powered.
                if (wasEmpty) {
                    dirtyThrust();
                }
                notifyUpdate();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return getTotalEnergyStoredFe();
        }

        @Override
        public int getMaxEnergyStored() {
            return getTotalEnergyCapacityFe();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public IonThrusterBlockEntity(final BlockPos pos, final BlockState state) {
        super(PropulsionBlockEntities.ION_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    protected IonThrusterBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        // Ion thrusters should evaluate power/consumption every server tick so FE usage is stable and responsive.
        if (requiresPerTickEnergyUpdate()) {
            this.isThrustDirty = true;
        }
        super.tick();
    }

    protected boolean requiresPerTickEnergyUpdate() {
        return true;
    }

    @Override
    protected boolean usesFluidFuelAccounting() {
        return false;
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {
        // Keep ion/vector tankless, but still inherit base behaviours like CC + plume damage.
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
        behaviours.add(new ThrusterDamager(this));
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        if (!isController() && isMultiblock()) {
            setThrustAndSync(0.0f);
            isThrustDirty = false;
            return;
        }
        if (isMultiblock()) {
            updateMultiblockIonThrust();
            return;
        }

        float thrust = 0;
        float currentPower = getEffectiveThrottle();

        if (currentPower > 0 && energyStored > 0) {
            float thrustPercentage = getEffectiveThrustPercentage();

            if (thrustPercentage > 0) {
                int ticksElapsed = getThrustUpdateIntervalTicks();

                // Config value is FE/t at full throttle; scale by throttle and elapsed ticks.
                double requestedDrain = energyDrainAccumulator
                        + (double) ticksElapsed * thrustPercentage * PropulsionConfig.ION_THRUSTER_FE_PER_TICK_AT_FULL_THROTTLE.get();
                int totalDrain = (int) Math.floor(requestedDrain);
                energyDrainAccumulator = requestedDrain - totalDrain;

                int consumed = Math.min(energyStored, totalDrain);
                if (consumed > 0) {
                    energyStored -= consumed;
                }
                float consumptionRatio = IonThrusterEnergyMath.poweredFraction(energyStored + consumed, totalDrain, consumed);
                if (consumptionRatio > 0.0f) {
                    float baseThrustPn = (float) (PropulsionConfig.ION_THRUSTER_BASE_THRUST.get() * getThrustUnitsPerKn());
                    baseThrustPn *= (float) calculateAtmosphericFactor();
                    thrust = baseThrustPn * thrustPercentage * consumptionRatio;
                }
                lastConsumedFePerTick = ticksElapsed > 0 ? (double) consumed / (double) ticksElapsed : 0.0d;
            } else {
                lastConsumedFePerTick = 0.0d;
            }
        } else {
            lastConsumedFePerTick = 0.0d;
        }
        // Mark dirty if energy was depleted to force thrust recalculation
        if (energyStored == 0 && thrust == 0) {
            isThrustDirty = true;
        } else {
            isThrustDirty = false;
        }
        setThrustAndSync(thrust);
        // Sync energy/consumption values used in goggles.
        setChanged();
        notifyUpdate();
    }

    private void updateMultiblockIonThrust() {
        if (level == null) {
            isThrustDirty = false;
            return;
        }
        int n = width * width * width;
        float thrust = 0.0f;
        float currentPower = getEffectiveThrottle();

        if (currentPower > 0) {
            float thrustPercentage = getEffectiveThrustPercentage();
            if (thrustPercentage > 0) {
                int ticksElapsed = getThrustUpdateIntervalTicks();

                double requestedDrain = energyDrainAccumulator
                        + (double) ticksElapsed * thrustPercentage * PropulsionConfig.ION_THRUSTER_FE_PER_TICK_AT_FULL_THROTTLE.get() * n;
                int totalDrain = (int) Math.floor(requestedDrain);
                energyDrainAccumulator = requestedDrain - totalDrain;

                int energyBeforeDrain = getTotalEnergyStoredFe();
                int consumed = drainEnergyFromMultiblock(totalDrain);
                float consumptionRatio = IonThrusterEnergyMath.poweredFraction(energyBeforeDrain, totalDrain, consumed);
                if (consumptionRatio > 0.0f) {
                    float baseThrustPn = (float) (PropulsionConfig.ION_THRUSTER_BASE_THRUST.get() * getThrustUnitsPerKn());
                    baseThrustPn *= (float) calculateAtmosphericFactor();
                    thrust = baseThrustPn * thrustPercentage * consumptionRatio * n * getIonMultiblockThrustMultiplier(width);
                }
                lastConsumedFePerTick = ticksElapsed > 0 ? (double) consumed / (double) ticksElapsed : 0.0d;
            } else {
                lastConsumedFePerTick = 0.0d;
            }
        } else {
            lastConsumedFePerTick = 0.0d;
        }

        setThrustAndSync(thrust);
        syncMultiblockMemberTelemetry(lastConsumedFePerTick);
        isThrustDirty = false;
        setChanged();
        notifyUpdate();
    }

    private int drainEnergyFromMultiblock(int requested) {
        if (requested <= 0 || level == null) {
            return 0;
        }
        int remaining = requested;
        BlockPos origin = worldPosition;
        for (int x = 0; x < width && remaining > 0; x++) {
            for (int y = 0; y < width && remaining > 0; y++) {
                for (int z = 0; z < width && remaining > 0; z++) {
                    BlockEntity be = dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter.getBlockEntitySafe(level, origin.offset(x, y, z));
                    if (!(be instanceof IonThrusterBlockEntity ion)) {
                        continue;
                    }
                    int take = Math.min(ion.energyStored, remaining);
                    if (take > 0) {
                        ion.energyStored -= take;
                        ion.setChanged();
                        ion.notifyUpdate();
                        remaining -= take;
                    }
                }
            }
        }
        return requested - remaining;
    }

    private void syncMultiblockMemberTelemetry(double fePerTick) {
        if (level == null || width <= 1) {
            return;
        }
        BlockPos origin = worldPosition;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                for (int z = 0; z < width; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockEntity be = dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter.getBlockEntitySafe(level, origin.offset(x, y, z));
                    if (be instanceof IonThrusterBlockEntity ion) {
                        ion.getThrusterData().setThrust(0);
                        ion.lastConsumedFePerTick = fePerTick;
                        ion.isThrustDirty = false;
                    }
                }
            }
        }
    }

    private int insertEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            return ctrl instanceof IonThrusterBlockEntity ion ? ion.insertEnergy(maxReceive, simulate) : 0;
        }
        if (!isMultiblock() || level == null) {
            int accepted = Math.min(maxReceive, Math.max(0, getEnergyCapacity() - energyStored));
            if (!simulate && accepted > 0) {
                energyStored += accepted;
            }
            return accepted;
        }

        int remaining = maxReceive;
        BlockPos origin = worldPosition;
        for (int x = 0; x < width && remaining > 0; x++) {
            for (int y = 0; y < width && remaining > 0; y++) {
                for (int z = 0; z < width && remaining > 0; z++) {
                    BlockEntity be = dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter.getBlockEntitySafe(level, origin.offset(x, y, z));
                    if (!(be instanceof IonThrusterBlockEntity ion)) {
                        continue;
                    }
                    int accepted = Math.min(remaining, Math.max(0, ion.getEnergyCapacity() - ion.energyStored));
                    if (accepted > 0) {
                        if (!simulate) {
                            ion.energyStored += accepted;
                            ion.setChanged();
                            ion.notifyUpdate();
                        }
                        remaining -= accepted;
                    }
                }
            }
        }
        return maxReceive - remaining;
    }

    private static float getIonMultiblockThrustMultiplier(int cubeWidth) {
        if (cubeWidth == 2) return PropulsionConfig.ION_MULTIBLOCK_2X_THRUST_MULTIPLIER.get().floatValue();
        if (cubeWidth == 3) return PropulsionConfig.ION_MULTIBLOCK_3X_THRUST_MULTIPLIER.get().floatValue();
        return 1.0f;
    }

    @Override
    public FluidStack fluidStack() {
        return FluidStack.EMPTY;
    }

    @Override
    public boolean validFluid() {
        return true;
    }

    @Override
    public boolean isIon() {
        return true;
    }


    @Override
    protected boolean supportsMultiblock() {
        return true;
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        return new IonParticleData(List.of(), getDyeColor(), null);
    }

    @Override
    protected boolean isWorking() {
        return getTotalEnergyStoredFe() > 0;
    }

    @Override
    public IFluidHandler getFluidHandler(final Direction side) {
        return null;
    }

    public IEnergyStorage getEnergyHandler(final Direction side) {
        // Expose FE on every side for broad cable/interface compatibility.
        // Some networks probe or pull from arbitrary faces instead of the nominal input face.
        return this.energyHandler;
    }

    @Override
    public boolean isVisuallyActive() {
        return this.getEffectiveThrottle() > 0.0d
                && (this.getTotalEnergyStoredFe() > 0 || this.isFadingOut());
    }

    @Override
    public boolean tryConsumeFuelBucket(final Player player, final InteractionHand hand, final ItemStack heldStack) {
        return false;
    }

    @Override
    public int getFuelAmountMb() {
        return this.getTotalEnergyStoredFe();
    }

    @Override
    public int getFuelCapacityMb() {
        return this.getTotalEnergyCapacityFe();
    }

    @Override
    protected double getBaseThrust() {
        return PropulsionConfig.ION_THRUSTER_BASE_THRUST.get();
    }

    @Override
    protected double getRawThrustCap() {
        return PropulsionConfig.ION_THRUSTER_BASE_THRUST.get();
    }

    public int getEnergyStoredFe() {
        return this.getTotalEnergyStoredFe();
    }

    public int getEnergyCapacity() {
        return PropulsionConfig.ION_THRUSTER_ENERGY_CAPACITY_FE.get();
    }

    public int getTotalEnergyCapacityForComputer() {
        return this.getTotalEnergyCapacityFe();
    }

    protected Direction getEnergyInputSide() {
        return this.getFacing();
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (this.getThrottle() <= 0.0d) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered"))
                    .style(ChatFormatting.GOLD);
        }
        if (this.getTotalEnergyStoredFe() <= 0) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.no_energy"))
                    .style(ChatFormatting.RED);
        }
        return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working"))
                .style(ChatFormatting.GREEN);
    }

    @Override
    protected void addThrusterDetails(final List<Component> tooltip, final boolean isPlayerSneaking) {
        addIonThrusterOutputDetails(tooltip);
        if (isMultiblock()) {
            int bonusPct = Math.round((getIonMultiblockThrustMultiplier(width) - 1.0f) * 100.0f);
            if (bonusPct > 0) {
                CreateLang.builder()
                        .add(Component.translatable("createpropulsion.gui.goggles.thruster.thrust_bonus"))
                        .text(": ")
                        .add(Component.literal("+" + bonusPct + "%").withStyle(ChatFormatting.AQUA))
                        .style(ChatFormatting.WHITE)
                        .forGoggles(tooltip);
            }
        }
        
        // Label line: "Energy Storage:"
        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.energy_container"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        // Storage line: "  100 / 1000 FE"
        CreateLang.builder()
                .add(Component.literal("  "))
                .add(Component.literal(Integer.toString(this.getTotalEnergyStoredFe())).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .add(Component.literal(Integer.toString(this.getTotalEnergyCapacityFe())).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" FE").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);

        // Consumption line: "  0.0 FE/t"
        CreateLang.builder()
                .add(Component.literal("  "))
                .add(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", this.lastConsumedFePerTick)).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" FE/t").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);
    }

    protected void addIonThrusterOutputDetails(final List<Component> tooltip) {
        float obstructionEfficiency = 100;
        ChatFormatting tooltipColor = ChatFormatting.GREEN;
        int scanLength = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
        if (getEmptyBlocks() < scanLength) {
            obstructionEfficiency = calculateObstructionEffect() * 100;
            tooltipColor = GoggleUtils.efficiencyColor(obstructionEfficiency);
            CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed"))
                    .space()
                    .add(CreateLang.text(GoggleUtils.makeObstructionBar(getEmptyBlocks(), scanLength)))
                    .style(tooltipColor)
                    .forGoggles(tooltip);
        }

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.efficiency")).text(": ")
                .add(CreateLang.number(obstructionEfficiency))
                .add(CreateLang.text("%"))
                .style(tooltipColor)
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.thrust_output"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.literal("  "))
                .add(Component.translatable("createpropulsion.tooltip.thrust1").withStyle(ChatFormatting.GRAY))
                .add(Component.literal(String.format(Locale.ROOT, "%.2f", this.getDisplayedThrustPnForTooltip() / getThrustUnitsPerKn())).withStyle(ChatFormatting.AQUA))
                .add(Component.literal(" pN").withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);
    }

    private int getTotalEnergyStoredFe() {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            return ctrl instanceof IonThrusterBlockEntity ion ? ion.getTotalEnergyStoredFe() : this.energyStored;
        }
        if (!isMultiblock() || level == null) {
            return this.energyStored;
        }
        int total = 0;
        BlockPos origin = worldPosition;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                for (int z = 0; z < width; z++) {
                    BlockEntity be = dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter.getBlockEntitySafe(level, origin.offset(x, y, z));
                    if (be instanceof IonThrusterBlockEntity ion) {
                        total += ion.energyStored;
                    }
                }
            }
        }
        return total;
    }

    private int getTotalEnergyCapacityFe() {
        if (!isController() && isMultiblock()) {
            ThrusterBlockEntity ctrl = getControllerBE();
            return ctrl instanceof IonThrusterBlockEntity ion ? ion.getTotalEnergyCapacityFe() : this.getEnergyCapacity();
        }
        int members = isMultiblock() ? width * width * width : 1;
        return this.getEnergyCapacity() * members;
    }

    @Override
    protected void write(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        tag.putInt("EnergyStored", this.energyStored);
        tag.putDouble("EnergyDrainAccumulator", this.energyDrainAccumulator);
        tag.putDouble("LastConsumedFePerTick", this.lastConsumedFePerTick);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(final CompoundTag tag, final HolderLookup.Provider registries, final boolean clientPacket) {
        this.energyStored = tag.getInt("EnergyStored");
        this.energyDrainAccumulator = tag.getDouble("EnergyDrainAccumulator");
        this.lastConsumedFePerTick = tag.getDouble("LastConsumedFePerTick");
        this.clampEnergyToCapacity();
        super.read(tag, registries, clientPacket);
        // Preserve multiblock connectivity state loaded by base class.
    }

    private void clampEnergyToCapacity() {
        this.energyStored = Math.clamp(this.energyStored, 0, this.getEnergyCapacity());
    }
}
