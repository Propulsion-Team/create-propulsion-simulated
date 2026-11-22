package dev.propulsionteam.propulsionsimulated.content.hydropump;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HydropumpSpeedBehaviour extends ScrollValueBehaviour {
    public HydropumpSpeedBehaviour(HydropumpBlockEntity pump) {
        super(Component.translatable("createpropulsion.hydropump.pumping_speed"), pump, new OutletValueBox());
        between(1, maxSpeed());
        value = maxSpeed();
        withFormatter(speed -> Integer.toString(speed));
        withCallback(speed -> pump.resetTransferBudget());
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        value = tag.contains("ScrollValue") ? Math.clamp(value, 1, maxSpeed()) : maxSpeed();
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, 100, 10, List.of(Component.literal("mB/20t")),
            new ValueSettingsFormatter(settings -> Component.literal(
                Integer.toString(sliderSpeed(settings.value())))));
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, Math.round((Math.clamp(getValue(), 1, maxSpeed()) - 1) * 100f / (maxSpeed() - 1)));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        between(1, maxSpeed());
        setValue(sliderSpeed(settings.value()));
        playFeedbackSound(this);
    }

    private static int sliderSpeed(int value) {
        return 1 + Math.round(Math.clamp(value, 0, 100) * (maxSpeed() - 1f) / 100f);
    }

    public static int maxSpeed() {
        return PropulsionConfig.HYDROPUMP_MAX_PUMPING_SPEED_MB.get();
    }

    private static class OutletValueBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 13.01);
        }

        @Override
        public float getScale() {
            return 0.375f;
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction side) {
            return side.getAxis() != state.getValue(HydropumpBlock.FACING).getAxis();
        }
    }
}
