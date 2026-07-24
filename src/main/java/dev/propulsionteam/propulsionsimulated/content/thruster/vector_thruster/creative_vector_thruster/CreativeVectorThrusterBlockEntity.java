package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.SimulatedThrustAdapter;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterPowerScrollValueBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plasma.PlasmaParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plume.PlumeParticleData;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.utility.GoggleUtils;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

public class CreativeVectorThrusterBlockEntity extends VectorThrusterBlockEntity {
    private CreativeThrusterPowerScrollValueBehaviour powerBehaviour;
    private CreativeThrusterBlockEntity.PlumeType plumeType = CreativeThrusterBlockEntity.PlumeType.PLASMA;
    private float peripheralThrustOutput = -1.0f;

    public CreativeVectorThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.CREATIVE_VECTOR_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        ValueBoxTransform slot = new CreativeVectorThrusterValueBox(true);
        powerBehaviour = new CreativeThrusterPowerScrollValueBehaviour(this, slot, () -> PropulsionConfig.CREATIVE_VECTOR_THRUSTER_MAX_THRUST.get());
        double base = PropulsionConfig.CREATIVE_VECTOR_THRUSTER_BASE_THRUST.get();
        double max = PropulsionConfig.CREATIVE_VECTOR_THRUSTER_MAX_THRUST.get();
        int startStep = (int) Math.round((base / max) * (CreativeThrusterPowerScrollValueBehaviour.TOTAL_STEPS - 1));
        powerBehaviour.value = Math.max(0, Math.min(CreativeThrusterPowerScrollValueBehaviour.TOTAL_STEPS - 1, startStep));
        powerBehaviour.withCallback(i -> {
            updateThrust(getBlockState());
            sendData();
        });
        behaviours.add(powerBehaviour);
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        float thrust = 0;
        float currentPower = getEffectiveThrottle();
        if (currentPower > 0) {
            float baseThrustPn = peripheralThrustOutput >= 0.0f ? peripheralThrustOutput : (float) (powerBehaviour.getTargetThrust() * getThrustUnitsPerKn());
            baseThrustPn *= (float) calculateAtmosphericFactor();
            thrust = currentPower * baseThrustPn;
        }
        setThrustAndSync(thrust);
        isThrustDirty = false;
    }

    @Override
    protected boolean isWorking() {
        return true;
    }

    @Override
    public boolean isCreative() {
        return true;
    }


    @Override
    public CreativeThrusterBlockEntity.PlumeType getPlumeType() {
        return plumeType;
    }

    @Override
    public boolean shouldEmitPlume() {
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.NONE)
            return false;
        if (!isPowered())
            return false;
        return hasPlumeSpace();
    }

    @Override
    public boolean isVisuallyActive() {
        return shouldEmitPlume();
    }

    private boolean hasPlumeSpace() {
        if (level == null)
            return false;

        Direction facing = getBlockState().getValue(CreativeVectorThrusterBlock.FACING);
        BlockPos plumeOccupiedPosition = worldPosition.relative(facing.getOpposite());
        return !SimulatedThrustAdapter.getBlockStateSafe(level, plumeOccupiedPosition).isFaceSturdy(level, plumeOccupiedPosition, facing);
    }

    public void cyclePlumeType() {
        int ordinal = plumeType.ordinal() + 1;
        if (ordinal >= CreativeThrusterBlockEntity.PlumeType.values().length) {
            ordinal = 0;
        }
        plumeType = CreativeThrusterBlockEntity.PlumeType.values()[ordinal];
        setChanged();
        sendData();
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.PLASMA) {
            return new PlasmaParticleData(List.of(), getDyeColor());
        }
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.ION) {
            return new IonParticleData(List.of(), getDyeColor(), null);
        }
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.PLUME) {
            return new PlumeParticleData(List.of(), getDyeColor());
        }
        return new PlumeParticleData(List.of(), getDyeColor());
    }

    /** Creative-vector Ion mode retains the vector nozzle's redstone-driven particle narrowing. */
    public ParticleOptions createCreativeVectorPlumeParticleOptions() {
        if (plumeType == CreativeThrusterBlockEntity.PlumeType.ION) {
            float size = Mth.lerp(getInterpolatedFlapProgress(1.0f), 0.85f, 0.35f);
            return new IonParticleData(List.of(), getDyeColor(), size);
        }
        return createParticleOptions();
    }



    @Override
    protected LangBuilder getGoggleStatus() {
        if (isPowered()) {
            return CreateLang.builder()
                    .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working"))
                    .style(ChatFormatting.GREEN);
        }
        return CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered"))
                .style(ChatFormatting.GOLD);
    }

    @Override
    protected double getBaseThrust() {
        if (peripheralThrustOutput >= 0.0f) {
            return (float) (peripheralThrustOutput / getThrustUnitsPerKn());
        }
        return powerBehaviour.getTargetThrust();
    }

    @Override
    protected double getRawThrustCap() {
        if (peripheralThrustOutput >= 0.0f) {
            return (float) (peripheralThrustOutput / getThrustUnitsPerKn());
        }
        return powerBehaviour.getTargetThrust();
    }

    /**
     * Overrides base thrust from the scroll when {@code >= 0} (pN). Pass a negative value or use
     * {@link #clearPeripheralThrustOutput()} to use scroll thrust again. Values are clamped to
     * {@link PropulsionConfig#CREATIVE_VECTOR_THRUSTER_MAX_THRUST} (kN) converted to pN.
     */
    public void setThrustOutput(float thrustOutputPn) {
        if (thrustOutputPn < 0.0f || Float.isNaN(thrustOutputPn)) {
            this.peripheralThrustOutput = -1.0f;
        } else {
            float maxPn = (float) (PropulsionConfig.CREATIVE_VECTOR_THRUSTER_MAX_THRUST.get() * getThrustUnitsPerKn());
            this.peripheralThrustOutput = Math.min(Math.max(0.0f, thrustOutputPn), maxPn);
        }
        updateThrust(getBlockState());
        setChanged();
        notifyUpdate();
    }

    public boolean hasPeripheralThrustOverride() {
        return peripheralThrustOutput >= 0.0f;
    }

    public void clearPeripheralThrustOutput() {
        setThrustOutput(-1.0f);
    }

    @Override
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
        this.emptyBlocks = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        float obstructionEfficiency = 100;
        ChatFormatting tooltipColor = ChatFormatting.GREEN;
        int scanLength = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
        if (emptyBlocks < scanLength) {
            obstructionEfficiency = calculateObstructionEffect() * 100;
            tooltipColor = GoggleUtils.efficiencyColor(obstructionEfficiency);
            CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed")).space()
                    .add(CreateLang.text(GoggleUtils.makeObstructionBar(emptyBlocks, scanLength))).style(tooltipColor).forGoggles(tooltip);
        }

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.efficiency")).text(": ")
                .add(CreateLang.number(obstructionEfficiency)).add(CreateLang.text("%"))
                .style(tooltipColor).forGoggles(tooltip);

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

        addParticleCategory(tooltip);
    }

    private void addParticleCategory(List<Component> tooltip) {
        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        Component particleValue = switch (plumeType) {
            case PLASMA -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.plasma")
                    .withStyle(ChatFormatting.AQUA);
            case ION -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.ion")
                    .withStyle(ChatFormatting.BLUE);
            case PLUME -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.plume")
                    .withStyle(ChatFormatting.GOLD);
            case NONE -> Component.translatable("createpropulsion.gui.goggles.creative_thruster.particle.none")
                    .withStyle(ChatFormatting.DARK_GRAY);
        };

        CreateLang.builder()
                .add(Component.literal("  "))
                .add(particleValue)
                .forGoggles(tooltip);
    }

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("plumeType", plumeType.ordinal());
        compound.putFloat("PeripheralThrustOutput", peripheralThrustOutput);
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (compound.contains("plumeType")) {
            int idx = compound.getInt("plumeType");
            plumeType = CreativeThrusterBlockEntity.PlumeType.values()[Mth.clamp(idx, 0, CreativeThrusterBlockEntity.PlumeType.values().length - 1)];
        }
        if (compound.contains("PeripheralThrustOutput")) {
            peripheralThrustOutput = Math.max(-1.0f, compound.getFloat("PeripheralThrustOutput"));
            if (peripheralThrustOutput >= 0.0f) {
                float maxPn = (float) (PropulsionConfig.CREATIVE_VECTOR_THRUSTER_MAX_THRUST.get() * getThrustUnitsPerKn());
                peripheralThrustOutput = Math.min(peripheralThrustOutput, maxPn);
            }
        }
    }
}
