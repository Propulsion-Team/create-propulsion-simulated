package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class AdvancedTiltAdapterAngleScrollBehaviour extends ScrollValueBehaviour {
    public static final BehaviourType<AdvancedTiltAdapterAngleScrollBehaviour> LEFT_TYPE = new BehaviourType<>();
    public static final BehaviourType<AdvancedTiltAdapterAngleScrollBehaviour> RIGHT_TYPE = new BehaviourType<>();

    /** {@code true} = left value box on the top face (left limit). */
    private final boolean controlsLeftLimit;

    public AdvancedTiltAdapterAngleScrollBehaviour(Component label, SmartBlockEntity be, boolean controlsLeftLimit) {
        // The control for each limit is intentionally shown on the opposite side.
        super(label, be, new AdvancedTiltAdapterAngleValueBox(!controlsLeftLimit));
        this.controlsLeftLimit = controlsLeftLimit;
        between(0, getMaximumAngle());
        withFormatter(v -> v + "\u00b0");
    }

    public void setStoredValue(int storedValue) {
        value = clampToConfiguredRange(storedValue);
    }

    public static int getMaximumAngle() {
        return PropulsionConfig.ADVANCED_TILT_ADAPTER_MAX_ANGLE.get();
    }

    public static int clampToConfiguredRange(int angle) {
        return Mth.clamp(angle, 0, getMaximumAngle());
    }

    private String nbtKey() {
        return controlsLeftLimit ? "LeftScrollValue" : "RightScrollValue";
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        nbt.putInt(nbtKey(), value);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        if (nbt.contains(nbtKey())) {
            setStoredValue(nbt.getInt(nbtKey()));
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return controlsLeftLimit ? LEFT_TYPE : RIGHT_TYPE;
    }

    /** Create routes value-setting packets by {@code netId}; must differ per side. */
    @Override
    public int netId() {
        return controlsLeftLimit ? 0 : 1;
    }

    @Override
    public void setValue(int newValue) {
        int clamped = clampToConfiguredRange(newValue);
        if (getValue() == clamped) {
            return;
        }
        value = clamped;
        if (blockEntity instanceof AdvancedTiltAdapterBlockEntity advanced) {
            advanced.onAngleLimitChanged(controlsLeftLimit, clamped);
        } else {
            blockEntity.setChanged();
            blockEntity.sendData();
        }
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> row = ImmutableList.of(Component.literal("\u00b0"));
        int maximumAngle = getMaximumAngle();
        int milestoneInterval = Math.max(1, maximumAngle / 10);
        return new ValueSettingsBoard(label, maximumAngle, milestoneInterval, row,
            new ValueSettingsFormatter(this::formatBoardValue));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        if (ctrlHeld && blockEntity instanceof AdvancedTiltAdapterBlockEntity advanced) {
            advanced.setSharedAngles(!advanced.areAnglesShared());
            playFeedbackSound(this);
            return;
        }
        int newValue = clampToConfiguredRange(valueSetting.value());
        if (getValue() == newValue) {
            return;
        }
        setValue(newValue);
        playFeedbackSound(this);
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value);
    }

    public MutableComponent formatBoardValue(ValueSettings settings) {
        return CreateLang.number(settings.value()).add(CreateLang.text("\u00b0")).component();
    }
}
