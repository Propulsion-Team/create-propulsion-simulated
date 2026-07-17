package dev.propulsionteam.propulsionsimulated.client.render.plume;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class ThrusterVisualEffects {
    private static final Map<AbstractThrusterBlockEntity, Float> VISUAL_POWER = new WeakHashMap<>();

    private ThrusterVisualEffects() {
    }

    public enum Preset {
        FIRE,
        SOLID,
        SUPERHEATED_SOLID,
        ION,
        VECTOR,
        PLASMA,
        CREATIVE
    }

    public static void render(AbstractThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, Preset preset, float forcedTargetPower) {
        if (be == null || be.getLevel() == null || be.isRemoved()) return;

        float targetPower = be.getUnobstructedBlocks() > 0
                ? Mth.clamp(forcedTargetPower, 0.0f, 1.0f)
                : 0.0f;

        renderInternal(be, partialTicks, ms, buffer, preset, targetPower);
    }

    public static void render(AbstractThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, Preset preset) {
        if (be == null || be.getLevel() == null || be.isRemoved()) return;

        float targetPower = be.isVisuallyActive() && be.getUnobstructedBlocks() > 0
                ? Mth.clamp(be.getPower(), 0.0f, 1.0f)
                : 0.0f;

        renderInternal(be, partialTicks, ms, buffer, preset, targetPower);
    }

    private static void renderInternal(AbstractThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, Preset preset, float targetPower) {
        float oldPower = VISUAL_POWER.getOrDefault(be, 0.0f);
        float speed = targetPower > oldPower ? getRiseSpeed(preset) : getFallSpeed(preset);
        float power = oldPower + (targetPower - oldPower) * speed;

        if (power < 0.002f) {
            VISUAL_POWER.remove(be);
            return;
        }

        VISUAL_POWER.put(be, power);

        Vec3 exhaust = be.getParticleDebugExhaustDirectionLocal();
        if (exhaust.lengthSqr() <= 1.0e-8d) return;
        exhaust = exhaust.normalize();

        Direction fallbackDirection = Direction.getNearest(exhaust.x, exhaust.y, exhaust.z);

        Vec3 localNozzle = be.getParticleDebugNozzlePositionLocal()
                .subtract(Vec3.atLowerCornerOf(be.getBlockPos()));

        int width = Math.max(1, be.width);
        float thrustNorm = Mth.clamp(be.getCurrentThrust() / 600000.0f, 0.0f, 1.0f);
        float time = (be.getLevel().getGameTime() + partialTicks) / 20.0f;

        float pulse = switch (preset) {
            case ION -> 0.995f + 0.004f * Mth.sin(time * 75.0f);
            default -> 0.985f + 0.010f * Mth.sin(time * 42.0f) + 0.006f * Mth.sin(time * 97.0f);
        };

        float length = getBaseLength(preset, power)
                * Mth.lerp(thrustNorm, 0.92f, 1.22f)
                * pulse
                * getWidthLengthScale(width, preset);

        float radius = getBaseRadius(preset, power)
                * Mth.lerp(thrustNorm, 0.92f, 1.08f)
                * getWidthRadiusScale(width, preset);

        ms.pushPose();

        if (preset == Preset.ION) {
            Vec3 nudge = exhaust.scale(-0.28d);
            ms.translate(
                    localNozzle.x + nudge.x,
                    localNozzle.y + nudge.y,
                    localNozzle.z + nudge.z
            );
        } else {
            ms.translate(localNozzle.x, localNozzle.y, localNozzle.z);
        }

        PlumeRenderer.render(
                ms,
                buffer,
                paramsFor(preset, fallbackDirection, power, length, radius),
                time,
                exhaust
        );

        ms.popPose();
    }

    public static Preset presetForCreativePlume(CreativeThrusterBlockEntity.PlumeType plumeType) {
        return switch (plumeType) {
            case PLASMA -> Preset.PLASMA;
            case ION -> Preset.ION;
            case PLUME -> Preset.FIRE;
            case NONE -> Preset.CREATIVE;
        };
    }

    private static PlumeRenderParams paramsFor(Preset preset, Direction exhaust, float power, float length, float radius) {
        return switch (preset) {
            case FIRE -> new PlumeRenderParams(
                    exhaust,
                    power,
                    length,
                    radius,
                    1.0f,
                    1.0f,
                    0.38f,
                    0.045f,
                    PlumeShape.SQUARE
            );
            case SOLID -> new PlumeRenderParams(
                    exhaust,
                    power,
                    length,
                    radius,
                    1.0f,
                    1.0f,
                    0.25f,
                    0.025f,
                    PlumeShape.SQUARE
            );
            case ION -> new PlumeRenderParams(
                    exhaust,
                    power,
                    length,
                    radius,
                    1.0f,
                    0.28f,
                    0.48f,
                    1.0f,
                    PlumeShape.ION_FULL
            );
            case PLASMA -> new PlumeRenderParams(
                    exhaust,
                    power,
                    length,
                    radius,
                    0.82f,
                    0.78f,
                    0.30f,
                    1.0f,
                    PlumeShape.ROUND
            );
            case CREATIVE -> new PlumeRenderParams(
                    exhaust,
                    power,
                    length,
                    radius,
                    0.75f,
                    0.80f,
                    0.35f,
                    1.0f,
                    PlumeShape.ROUND
            );
            case VECTOR -> new PlumeRenderParams(
                    exhaust,
                    power,
                    length,
                    radius,
                    0.92f,
                    0.62f,
                    0.28f,
                    1.0f,
                    PlumeShape.ROUND
            );
            case SUPERHEATED_SOLID -> new PlumeRenderParams(
                    exhaust,
                    power,
                    length,
                    radius,
                    1.0f,
                    0.12f,
                    0.48f,
                    1.0f,
                    PlumeShape.SQUARE
            );
        };
    }

    private static float getBaseLength(Preset preset, float power) {
        return switch (preset) {
            case FIRE -> Mth.lerp(power, 3.2f, 9.4f);
            case SOLID -> Mth.lerp(power, 2.2f, 6.6f);
            case ION -> Mth.lerp(power, 0.95f, 2.15f);
            case PLASMA -> Mth.lerp(power, 3.2f, 9.0f);
            case CREATIVE -> Mth.lerp(power, 3.2f, 8.5f);
            case VECTOR -> Mth.lerp(power, 2.8f, 7.4f);
            case SUPERHEATED_SOLID -> Mth.lerp(power, 2.4f, 6.8f);
        };
    }

    private static float getBaseRadius(Preset preset, float power) {
        return switch (preset) {
            case FIRE -> Mth.lerp(power, 0.075f, 0.245f);
            case SOLID -> Mth.lerp(power, 0.120f, 0.36f);
            case ION -> Mth.lerp(power, 0.34f, 0.46f);
            case PLASMA -> Mth.lerp(power, 0.045f, 0.150f);
            case CREATIVE -> Mth.lerp(power, 0.060f, 0.18f);
            case VECTOR -> Mth.lerp(power, 0.040f, 0.135f);
            case SUPERHEATED_SOLID -> Mth.lerp(power, 0.070f, 0.185f);
        };
    }

    private static float getWidthLengthScale(int width, Preset preset) {
        if (preset == Preset.ION) {
            return switch (width) {
                case 1 -> 1.0f;
                case 2 -> 1.15f;
                default -> 1.35f;
            };
        }

        if (preset == Preset.VECTOR) {
            return switch (width) {
                case 1 -> 1.0f;
                case 2 -> 1.22f;
                default -> 1.42f;
            };
        }

        return switch (width) {
            case 1 -> 1.0f;
            case 2 -> 1.42f;
            default -> 1.85f;
        };
    }

    private static float getWidthRadiusScale(int width, Preset preset) {
        if (preset == Preset.ION) {
            return switch (width) {
                case 1 -> 1.0f;
                case 2 -> 1.08f;
                default -> 1.16f;
            };
        }

        if (preset == Preset.VECTOR) {
            return switch (width) {
                case 1 -> 1.45f;
                case 2 -> 1.95f;
                default -> 2.55f;
            };
        }

        return switch (width) {
            case 1 -> 1.0f;
            case 2 -> 2.25f;
            default -> 3.55f;
        };
    }

    private static float getRiseSpeed(Preset preset) {
        return switch (preset) {
            case ION -> 0.28f;
            default -> 0.34f;
        };
    }

    private static float getFallSpeed(Preset preset) {
        return switch (preset) {
            case ION -> 0.18f;
            default -> 0.22f;
        };
    }
}
