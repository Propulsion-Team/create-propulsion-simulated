package dev.propulsionteam.propulsionsimulated.content.heat.engine;

import java.util.List;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerBehaviour;
import dev.propulsionteam.propulsionsimulated.content.heat.IHeatConsumer;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class StirlingEngineBlockEntity extends GeneratingKineticBlockEntity implements IHeatConsumer {
    public static final float MAX_GENERATED_RPM = 256.0f;
    public static final float HEAT_CONSUMPTION_RATE = 1.0f;

    protected StirlingScrollValueBehaviour targetSpeedBehaviour;
    private int activeTicks = 0;
    private boolean firstTick = true;

    private boolean isPowered = false;
    private boolean computerActive = true;
    private boolean wasEngineActive = true;
    private float configuredStressCapacity;

    public AbstractComputerBehaviour computerBehaviour;

    public StirlingEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public StirlingEngineBlockEntity(BlockPos pos, BlockState state) {
        this(PropulsionBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        wasEngineActive = isEngineActive();
        if (activeTicks > 0 || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
    }

    public StirlingScrollValueBehaviour getTargetSpeedBehaviour() { return targetSpeedBehaviour; }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviors) {
        super.addBehaviours(behaviors);
        targetSpeedBehaviour = new StirlingScrollValueBehaviour(Component.translatable("createpropulsion.stirling_engine.generated_speed"), this, new StirlingEngineValueBox());
        targetSpeedBehaviour.value = 4;
        targetSpeedBehaviour.withCallback(i -> this.updateGeneratedRotation());
        behaviors.add(targetSpeedBehaviour);

        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviors.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    public void setPowered(boolean powered) {
        this.isPowered = powered;
    }

    public void setComputerActive(boolean active) {
        this.computerActive = active;
    }

    public boolean isEngineActive() {
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour != null && computerBehaviour.hasAttachedComputer()) {
            return computerActive;
        }
        return !isPowered;
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) return;

        if (firstTick) {
            firstTick = false;
            isPowered = level.hasNeighborSignal(getBlockPos());
            wasEngineActive = isEngineActive();
            if (activeTicks > 0) {
                reActivateSource = true;
            }
        }

        float configuredCapacity = PropulsionConfig.STIRLING_GENERATED_SU.get().floatValue();
        if (configuredStressCapacity != configuredCapacity) {
            configuredStressCapacity = configuredCapacity;
            updateGeneratedRotation();
        }

        boolean currentlyActive = isEngineActive();
        if (wasEngineActive != currentlyActive) {
            wasEngineActive = currentlyActive;
            updateGeneratedRotation();
        }

        if (activeTicks > 0) {
            activeTicks--;
            if (activeTicks == 0) {
                updateGeneratedRotation();
            }
        }

        tickBlazeBurnerHeat();
    }

    private void tickBlazeBurnerHeat() {
        if (!isEngineActive()) return;
        if (!PropulsionConfig.BLAZE_BURNERS_HEAT_STIRLING_ENGINES.get()) return;

        BlockState below = level.getBlockState(worldPosition.below());
        if (!(below.getBlock() instanceof BlazeBurnerBlock)) return;
        if (!below.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)) return;
        if (!below.getValue(BlazeBurnerBlock.HEAT_LEVEL).isAtLeast(HeatLevel.FADING)) return;

        boolean wasInactive = activeTicks == 0;
        activeTicks = 3;
        if (wasInactive) {
            updateGeneratedRotation();
        }
    }

    @Override
    public boolean isActive() {
        return isEngineActive(); 
    }

    @Override
    public float getOperatingThreshold() {
        return 0.1f;
    }

    @Override
    public float consumeHeat(float maxAvailable, float expectedHeatOutput, boolean simulate) {
        if (!isEngineActive()) return 0f;

        float rpm = targetSpeedBehaviour.getUnsignedRPM();
        float modeConsumptionFactor = rpm / MAX_GENERATED_RPM;
        float toConsume = Math.min(modeConsumptionFactor * HEAT_CONSUMPTION_RATE, maxAvailable); //Do not care about expected heat output (we always consume less than it)

        if (!simulate && toConsume > 0) {
            boolean wasInactive = activeTicks == 0;
            activeTicks = 3;

            //We were off, but now we are activate -> update rotation
            if (wasInactive) {
                updateGeneratedRotation();
            }
        }

        return toConsume;
    }

    @Override
    public float getGeneratedSpeed() {
        if (!isEngineActive() || activeTicks <= 0) return 0f;
        int generatedRPM = targetSpeedBehaviour.getRPM();
        return convertToDirection(generatedRPM, getBlockState().getValue(StirlingEngineBlock.HORIZONTAL_FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (level != null && level.isClientSide) return lastCapacityProvided;
        if (!isEngineActive() || activeTicks <= 0) return lastCapacityProvided = 0f;
        float rpm = targetSpeedBehaviour.getUnsignedRPM();
        if (rpm == 0) return lastCapacityProvided = 0f;

        float stressFactor = MAX_GENERATED_RPM / rpm;
        float capacity = stressFactor * PropulsionConfig.STIRLING_GENERATED_SU.get().floatValue();

        this.lastCapacityProvided = capacity;
        return capacity;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
            .add(Component.translatable("createpropulsion.gui.goggles.title.generator_stats"))
            .style(ChatFormatting.WHITE)
            .forGoggles(tooltip);
        boolean active = isEngineActive();

        String status;
        ChatFormatting statusColor;
        if (active) {
            if (activeTicks == 0) {
                status = "createpropulsion.gui.goggles.stirling_engine.status.no_heat";
                statusColor = ChatFormatting.GOLD;
            } else {
                status = "createpropulsion.gui.goggles.stirling_engine.status.on";
                statusColor = ChatFormatting.GREEN;
            }
        } else {
            status = "createpropulsion.gui.goggles.stirling_engine.status.off";
            statusColor = ChatFormatting.RED;
        }

        CreateLang.builder()
            .add(Component.translatable("createpropulsion.gui.goggles.stirling_engine.status"))
            .text(": ")
            .add(Component.translatable(status).withStyle(statusColor))
            .forGoggles(tooltip);

        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour != null && computerBehaviour.hasAttachedComputer()) {
            CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.cc.peripheral_controlled"))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        }

        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("activeTicks", activeTicks);
        compound.putBoolean("isPowered", isPowered);
        compound.putBoolean("computerActive", computerActive);
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        activeTicks = compound.getInt("activeTicks");
        isPowered = compound.getBoolean("isPowered");
        if (compound.contains("computerActive")) {
            computerActive = compound.getBoolean("computerActive");
        } else {
            computerActive = true;
        }
    }
}

