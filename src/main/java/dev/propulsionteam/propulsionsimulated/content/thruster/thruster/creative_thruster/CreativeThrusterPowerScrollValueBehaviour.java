package dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import java.util.function.DoubleSupplier;

public class CreativeThrusterPowerScrollValueBehaviour extends ScrollValueBehaviour {
    public static final int TOTAL_STEPS = 100;
    private final DoubleSupplier maxThrustSupplier;

    private double getForcePerStep() {
        return maxThrustSupplier.getAsDouble() / (double) TOTAL_STEPS;
    }

    public float getTargetThrust() {
        return (float) ((value + 1) * getForcePerStep());
    }
 
    public CreativeThrusterPowerScrollValueBehaviour(SmartBlockEntity be) {
        this(be, new CreativeThrusterValueBox(), () -> PropulsionConfig.CREATIVE_THRUSTER_MAX_THRUST.get());
    }

    public CreativeThrusterPowerScrollValueBehaviour(SmartBlockEntity be, DoubleSupplier maxThrustSupplier) {
        this(be, new CreativeThrusterValueBox(), maxThrustSupplier);
    }

    public CreativeThrusterPowerScrollValueBehaviour(SmartBlockEntity be, com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform slot, DoubleSupplier maxThrustSupplier) {
        super(Component.translatable("createpropulsion.gui.creative_thruster.power_behaviour"), be, slot);
        this.maxThrustSupplier = maxThrustSupplier;
        between(0, TOTAL_STEPS - 1); //Why is this even a thing :\
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> row = ImmutableList.of(Component.translatable("createpropulsion.gui.goggles.thruster.unit_pn"));
        return new ValueSettingsBoard(label, TOTAL_STEPS - 1, 10, row, new ValueSettingsFormatter(this::formatBoardValue));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        int newValue = valueSetting.value();
        newValue = Math.max(0, Math.min(newValue, TOTAL_STEPS - 1));
        
        if (getValue() == newValue) return;

        setValue(newValue);
        playFeedbackSound(this);
    }

    @Override 
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value);
    }

    public MutableComponent formatBoardValue(ValueSettings settings) {
        double forceInKN = (settings.value() + 1) * getForcePerStep();
        return CreateLang.builder()
            .add(CreateLang.number((int) forceInKN))
            .add(Component.translatable("createpropulsion.gui.goggles.thruster.unit_pn"))
            .component();
    }

    @Override
    public String formatValue() {
        double forceInKN = (value + 1) * getForcePerStep();
        return String.valueOf((int) forceInKN);
    }
}
