package dev.propulsionteam.propulsionsimulated.content.heat.burners;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.utility.CreateLang;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.heat.HeatMapper;
import dev.propulsionteam.propulsionsimulated.content.heat.HeatSourceBehavior;
import dev.propulsionteam.propulsionsimulated.content.heat.IHeatConsumer;
import dev.propulsionteam.propulsionsimulated.content.heat.IHeatSource;
import dev.propulsionteam.propulsionsimulated.content.heat.HeatMapper.HeatLevelString;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractBurnerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final TagKey<Block> THERMOSTAT_TARGETS = BlockTags.create(
        ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "thermostat_targets"));
    protected HeatSourceBehavior heatSource;
    protected HeatLevelString heatLevelName = HeatLevelString.COLD;
    protected boolean isPowered = false;

    protected static final float PASSIVE_LOSS_PER_TICK = 0.05f;

    public AbstractBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        heatSource = new HeatSourceBehavior(this, getBaseHeatCapacity(), getHeatPerTick());
        behaviours.add(heatSource);
    }

    protected abstract float getBaseHeatCapacity();

    protected abstract Direction getHeatCapSide();

    protected abstract float getHeatPerTick();

    public void updatePoweredState() {
        if (level == null || level.isClientSide()) return;
        boolean currentlyPowered = level.getBestNeighborSignal(worldPosition) > 0;
        if (this.isPowered != currentlyPowered) {
            this.isPowered = currentlyPowered;
            notifyUpdate();
        }
    }

    protected void tickHeatPhysics(float heatGeneration) {
        float heatConsumedLastTick = offerHeatToConsumer();
        float passiveLoss = 0;

        if (heatConsumedLastTick == 0) {
            passiveLoss = heatSource.getHeatSource().getHeatStored() > 0 ? PASSIVE_LOSS_PER_TICK : 0f;
        }

        float netHeatChange = heatGeneration - passiveLoss - heatConsumedLastTick;
        IHeatSource cap = heatSource.getHeatSource();
        if (netHeatChange > 0) cap.generateHeat(netHeatChange);
        else cap.extractHeat(Math.abs(netHeatChange), false);
    }

    protected boolean shouldThermostatBurn() {
        if (isPowered) return true; //Redstone override
        if (level == null) return false;

        if (level.getBlockState(worldPosition.above()).getBlock() instanceof FluidTankBlock) return true;

        BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
        if (!(beAbove instanceof IHeatConsumer)
            && level.getBlockState(worldPosition.above()).is(THERMOSTAT_TARGETS)) return true;
        if (beAbove == null) return false;

        if (beAbove instanceof BasinBlockEntity && PropulsionConfig.BURNERS_POWER_HEATED_MIXERS.get()) return true;

        if (beAbove instanceof IHeatConsumer consumer) {
                if (!consumer.isActive()) return false;

                float thresholdPercent = consumer.getOperatingThreshold();
                float thresholdInHU = getBaseHeatCapacity() * thresholdPercent;

                float currentHeat = heatSource.getHeatSource().getHeatStored();
                float expectedHeatProduction = heatSource.getHeatSource().getExpectedHeatProduction();
                //Prediction logic
                float consumptionNextTick = consumer.consumeHeat(currentHeat, expectedHeatProduction, true); 
                float heatNextTick = currentHeat - consumptionNextTick - PASSIVE_LOSS_PER_TICK;

                return heatNextTick < thresholdInHU;
        }
        return false;
    }

    private float offerHeatToConsumer() {
        if (level == null) return 0f;
        BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
        if (beAbove == null) return 0f;
        
        if (beAbove instanceof IHeatConsumer consumer) {
                float availableHeat = heatSource.getHeatSource().getHeatStored();
                if (availableHeat <= 0) return 0f;

                float thresholdPercent = consumer.getOperatingThreshold();
                float thresholdInHU = getBaseHeatCapacity() * thresholdPercent;
                float expectedHeatProduction = heatSource.getHeatSource().getExpectedHeatProduction();

                if (consumer.isActive() && availableHeat >= thresholdInHU) {
                    return consumer.consumeHeat(availableHeat, expectedHeatProduction,  false);
                }
                return 0f;
        }
        return 0f;
    }

    protected void updateHeatLevelName() {
        HeatLevelString previousName = heatLevelName;
        float availableHeat = heatSource.getHeatSource().getHeatStored();
        float percentage = availableHeat / getBaseHeatCapacity();

        heatLevelName = HeatMapper.getHeatString(percentage);
        if (previousName != heatLevelName) {
            notifyUpdate();
        }
    }

    protected HeatLevel calculateHeatLevel() {
        IHeatSource cap = heatSource.getHeatSource();
        if (cap.getHeatStored() == 0) return HeatLevel.NONE;
        float percentage = cap.getHeatStored() / cap.getMaxHeatStored();
        return HeatMapper.getHeatLevel(percentage);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
            .add(Component.translatable("createpropulsion.gui.goggles.title.burner_stats"))
            .style(ChatFormatting.WHITE)
            .forGoggles(tooltip);
        addHeatInfoToTooltip(tooltip);
        addSpecificTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    protected abstract void addSpecificTooltip(List<Component> tooltip, boolean isPlayerSneaking);

    private void addHeatInfoToTooltip(List<Component> tooltip) {
        ChatFormatting color = null;
        String key = null;

        switch (heatLevelName) {
            case COLD: color = ChatFormatting.BLUE; key = "createpropulsion.gui.goggles.burner.heat.cold"; break;
            case WARM: color = ChatFormatting.GOLD; key = "createpropulsion.gui.goggles.burner.heat.warm"; break;
            case HOT: color = ChatFormatting.GOLD; key = "createpropulsion.gui.goggles.burner.heat.hot"; break;
            case SEARING: color = ChatFormatting.RED; key = "createpropulsion.gui.goggles.burner.heat.searing"; break;
            default: color = ChatFormatting.BLUE; break;
        }

        //Heat level
        if (key != null)
            CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.burner.status")).text(": ").add(CreateLang.builder().add(Component.translatable(key)).style(color)).forGoggles(tooltip);

        //Thermostat
        CreateLang.builder()
            .add(Component.translatable("createpropulsion.gui.goggles.burner.thermostat"))
            .text(": ")
            .add(CreateLang.builder().add(Component.translatable(!isPowered ? "createpropulsion.gui.goggles.burner.thermostat.on" : "createpropulsion.gui.goggles.burner.thermostat.off"))
                .style(!isPowered ? ChatFormatting.GREEN : ChatFormatting.RED))
            .forGoggles(tooltip);
    }

    //NBT

    @Override
    protected void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("heatLevelName", heatLevelName.name());
        tag.putBoolean("isPowered", isPowered);
    }

    @Override
    protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("heatLevelName"))
            heatLevelName = HeatLevelString.valueOf(tag.getString("heatLevelName"));
        isPowered = tag.getBoolean("isPowered");
    }
}
