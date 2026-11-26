package dev.propulsionteam.propulsionsimulated.content.propulsion_core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public record VectorCoreState(boolean enabled, boolean depleted, long changedAt, float phase, long flashedAt) {
    public static final int FADE_TICKS = 20;
    private static final int PHASE_PERIOD = 120;
    private static final int FLASH_TICKS = 14;

    public static final Codec<VectorCoreState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(VectorCoreState::enabled),
            Codec.BOOL.optionalFieldOf("depleted", false).forGetter(VectorCoreState::depleted),
            Codec.LONG.fieldOf("changed_at").forGetter(VectorCoreState::changedAt),
            Codec.FLOAT.fieldOf("phase").forGetter(VectorCoreState::phase),
            Codec.LONG.optionalFieldOf("flashed_at", 0L).forGetter(VectorCoreState::flashedAt)
    ).apply(instance, VectorCoreState::new));

    public static final StreamCodec<ByteBuf, VectorCoreState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, VectorCoreState::enabled,
            ByteBufCodecs.BOOL, VectorCoreState::depleted,
            ByteBufCodecs.VAR_LONG, VectorCoreState::changedAt,
            ByteBufCodecs.FLOAT, VectorCoreState::phase,
            ByteBufCodecs.VAR_LONG, VectorCoreState::flashedAt,
            VectorCoreState::new);

    public static boolean isEnabled(ItemStack stack) {
        VectorCoreState state = get(stack);
        return state == null || state.enabled;
    }

    public static boolean isDepleted(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    public static boolean isWorking(ItemStack stack) {
        return isEnabled(stack) && !isDepleted(stack);
    }

    public static boolean showsDepleted(ItemStack stack) {
        VectorCoreState state = get(stack);
        return state == null ? isDepleted(stack) : state.depleted;
    }

    public static void setEnabled(ItemStack stack, boolean enabled, long gameTime) {
        VectorCoreState state = get(stack);
        update(stack, enabled, state != null && state.depleted, gameTime);
    }

    public static boolean syncDepleted(ItemStack stack, long gameTime) {
        return update(stack, isEnabled(stack), isDepleted(stack), gameTime);
    }

    public static float phaseAt(ItemStack stack, double time) {
        VectorCoreState state = get(stack);
        if (state != null) return state.phaseAt(time);
        return isDepleted(stack) ? 0.0f : wrap(time);
    }

    public static float flashAt(ItemStack stack, double time) {
        VectorCoreState state = get(stack);
        if (state == null) return 0.0f;
        double elapsed = time - state.flashedAt;
        if (elapsed < -FLASH_TICKS) return 0.0f;
        return (float) Mth.clamp(1.0d - elapsed / FLASH_TICKS, 0.0d, 1.0d);
    }

    private static VectorCoreState get(ItemStack stack) {
        return stack.get(PropulsionDataComponents.VECTOR_CORE_STATE.get());
    }

    private static boolean update(ItemStack stack, boolean enabled, boolean depleted, long gameTime) {
        VectorCoreState old = get(stack);
        if (old == null) {
            old = new VectorCoreState(true, false, gameTime - FADE_TICKS,
                    wrap(gameTime - travel(true, FADE_TICKS)), 0L);
        }
        if (old.enabled == enabled && old.depleted == depleted) return false;
        long changedAt = old.changedAt;
        float phase = old.phase;
        boolean spinning = enabled && !depleted;
        if (spinning != old.spinning()) {
            long elapsed = Mth.clamp(gameTime - old.changedAt, 0, FADE_TICKS);
            changedAt = gameTime - (FADE_TICKS - elapsed);
            phase = wrap(old.phaseAt(gameTime) - travel(spinning, gameTime - changedAt));
        }
        stack.set(PropulsionDataComponents.VECTOR_CORE_STATE.get(),
                new VectorCoreState(enabled, depleted, changedAt, phase, gameTime));
        return true;
    }

    private boolean spinning() {
        return enabled && !depleted;
    }

    private float phaseAt(double time) {
        return wrap(phase + travel(spinning(), time - changedAt));
    }

    private static double travel(boolean spinning, double elapsed) {
        double fade = Mth.clamp(elapsed, 0.0d, FADE_TICKS);
        double rampUp = fade * fade / (2 * FADE_TICKS);
        return spinning ? rampUp + Math.max(0.0d, elapsed - FADE_TICKS) : fade - rampUp;
    }

    private static float wrap(double phase) {
        return (float) Mth.positiveModulo(phase, (double) PHASE_PERIOD);
    }
}
