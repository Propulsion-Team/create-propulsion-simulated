package dev.propulsionteam.propulsionsimulated.client.render.plume;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster.CreativeVectorThrusterBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

public final class ThrusterVisualEffects {
    private static final Map<AbstractThrusterBlockEntity, VisualState> VISUAL_STATES = new WeakHashMap<>();

    private static final class VisualState {
        float power;
        float burst;
        long nanos;
    }

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
        VisualState state = VISUAL_STATES.get(be);
        if (state == null) {
            if (targetPower <= 0.0f) return;
            state = new VisualState();
            VISUAL_STATES.put(be, state);
        }
        long nowNanos = System.nanoTime();
        float dt = state.nanos == 0L ? 1.0f / 60.0f : Math.min(0.25f, (nowNanos - state.nanos) / 1.0e9f);
        state.nanos = nowNanos;

        float explosive = Mth.clamp((targetPower - 0.35f) / 0.5f, 0.0f, 1.0f);
        explosive = explosive * explosive * (3.0f - 2.0f * explosive);
        if (targetPower > state.power && state.power < 0.03f) {
            state.burst = Math.max(state.burst, explosive);
        }
        state.burst = Math.max(0.0f, state.burst - dt / 0.7f);

        float rate = targetPower > state.power
                ? getRiseRate(preset) * (1.0f + 1.2f * explosive)
                : getFallRate(preset);
        float power = state.power + (targetPower - state.power) * (1.0f - (float) Math.exp(-rate * dt));

        if (power < 0.002f && targetPower <= 0.0f) {
            VISUAL_STATES.remove(be);
            return;
        }

        state.power = power;
        float flare = state.burst;
        float settle = 1.0f - Mth.clamp(Math.abs(targetPower - power) * 12.0f, 0.0f, 1.0f);

        Vec3 exhaust = be.getRenderExhaustDirectionLocal(partialTicks);
        if (exhaust.lengthSqr() <= 1.0e-8d) return;
        exhaust = exhaust.normalize();

        Direction fallbackDirection = Direction.getNearest(exhaust.x, exhaust.y, exhaust.z);

        Vec3 localNozzle = be.getRenderNozzlePositionLocal(partialTicks)
                .subtract(Vec3.atLowerCornerOf(be.getBlockPos()));

        int width = Math.max(1, be.width);
        float thrustNorm = Mth.clamp(be.getCurrentThrust() / 600000.0f, 0.0f, 1.0f);
        float time = (be.getLevel().getGameTime() + partialTicks) / 20.0f;

        float pulse = switch (preset) {
            case ION -> 0.995f + 0.004f * Mth.sin(time * 75.0f);
            default -> 0.985f + 0.010f * Mth.sin(time * 42.0f) + 0.006f * Mth.sin(time * 97.0f);
        };

        float length = preset == Preset.ION
                ? Mth.lerp(power, 2.0f, 4.0f) * pulse
                : getBaseLength(preset, power)
                * Mth.lerp(thrustNorm, 0.92f, 1.22f)
                * pulse
                * getWidthLengthScale(width, preset);

        float radius = nozzleRadius(be, preset, width);

        length *= 1.0f + 0.9f * flare;

        ms.pushPose();

        localNozzle = localNozzle.add(exhaust.scale(exhaustLead(be, width)));
        ms.translate(localNozzle.x, localNozzle.y, localNozzle.z);

        float capRadius = Math.min(radius, openingVoxels(be, width) / 32.0f);
        PlumeRenderParams params = paramsFor(preset, fallbackDirection, power, length, radius);
        params = new PlumeRenderParams(params.direction(), params.power(), params.length(), params.radius(),
                params.alpha(), params.red(), params.green(), params.blue(),
                boxShape(preset), be.getDyeColor(), capRadius, flare, settle);
        if (PropulsionConfig.useVeilLight()) {
            keepGlow(be, partialTicks, localNozzle, exhaust, params, time);
        }
        Quaternionf orientation = be instanceof VectorThrusterBlockEntity vector
                ? nozzleOrientation(vector, partialTicks, exhaust)
                : null;
        PlumeRenderer.render(
                ms,
                buffer,
                params,
                time,
                exhaust,
                orientation
        );

        ms.popPose();
    }

    private static Quaternionf nozzleOrientation(VectorThrusterBlockEntity be, float partialTicks, Vec3 exhaust) {
        var direction = be.getInterpolatedVectorDirection(partialTicks);
        float yaw = (float) Math.atan2(direction.x, direction.z);
        float pitch = (float) -Math.asin(Mth.clamp(direction.y, -1.0d, 1.0d));
        Direction facing = be.getFacing();
        if (facing == Direction.UP) {
            yaw = -yaw;
            pitch = -pitch;
        }

        Quaternionf facingRotation = switch (facing) {
            case SOUTH -> Axis.YP.rotationDegrees(-180.0f);
            case WEST -> Axis.YP.rotationDegrees(-270.0f);
            case EAST -> Axis.YP.rotationDegrees(-90.0f);
            case UP -> Axis.XP.rotationDegrees(-270.0f);
            case DOWN -> Axis.XP.rotationDegrees(-90.0f);
            default -> new Quaternionf();
        };

        Quaternionf orientation = facingRotation
                .mul(Axis.YP.rotation(yaw))
                .mul(Axis.XP.rotation(pitch))
                .mul(new Quaternionf().rotationTo(0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f));

        Vector3f axis = orientation.transform(new Vector3f(0.0f, -1.0f, 0.0f));
        if (axis.dot((float) exhaust.x, (float) exhaust.y, (float) exhaust.z) < 0.98f) {
            return null;
        }
        return orientation;
    }

    private static float exhaustLead(AbstractThrusterBlockEntity be, int width) {
        float tip;
        if (be instanceof SolidFuelThrusterBlockEntity) {
            tip = 2.0f;
        } else if (be instanceof VectorThrusterBlockEntity) {
            tip = 4.0f;
        } else if (be instanceof IonThrusterBlockEntity) {
            tip = width * 8.0f;
        } else if (width == 1) {
            tip = be instanceof CreativeThrusterBlockEntity ? 2.0f : 6.0f;
        } else {
            tip = width * 8.0f + (width == 2 ? -2.0f : 0.0f);
        }
        float nozzle = width > 1 ? width * 8.0f + 7.2f : (float) be.getNozzleOffsetFromCenter() * 16.0f;
        return (tip - nozzle) / 16.0f;
    }

    private static float openingVoxels(AbstractThrusterBlockEntity be, int width) {
        if (be instanceof SolidFuelThrusterBlockEntity) return 10.0f;
        if (be instanceof VectorThrusterBlockEntity) return 10.0f;
        if (be instanceof IonThrusterBlockEntity) return 16.0f * width;
        if (be instanceof CreativeThrusterBlockEntity && width == 1) return 8.0f;
        return width == 1 ? 10.0f : width == 2 ? 22.0f : 34.0f;
    }

    private static float nozzleRadius(AbstractThrusterBlockEntity be, Preset preset, int width) {
        boolean creative = be instanceof CreativeThrusterBlockEntity || be instanceof CreativeVectorThrusterBlockEntity;
        int[] diameters = switch (preset) {
            case ION -> creative ? new int[]{8, 22, 36} : new int[]{14, 28, 44};
            case PLASMA, VECTOR, CREATIVE -> new int[]{8, 22, 36};
            default -> new int[]{10, 22, 36};
        };
        return Math.min(diameters[Mth.clamp(width, 1, 3) - 1], openingVoxels(be, width)) / 32.0f;
    }

    private static PlumeShape boxShape(Preset preset) {
        return switch (preset) {
            case PLASMA, VECTOR, CREATIVE -> PlumeShape.PLASMA_BOXES;
            default -> PlumeShape.FLAME_BOXES;
        };
    }

    private static void keepGlow(AbstractThrusterBlockEntity be, float partialTicks, Vec3 localNozzle, Vec3 exhaust, PlumeRenderParams params, float time) {
        Vec3 nozzle = localNozzle.add(Vec3.atLowerCornerOf(be.getBlockPos()));
        Vec3 ahead = nozzle.add(exhaust);
        SubLevel subLevel = Sable.HELPER.getContaining(be.getLevel(), be.getBlockPos());
        if (subLevel != null) {
            var pose = subLevel instanceof ClientSubLevel client ? client.renderPose() : subLevel.logicalPose();
            nozzle = pose.transformPosition(nozzle);
            ahead = pose.transformPosition(ahead);
        }
        Vec3 at = nozzle.add(ahead.subtract(nozzle).normalize());
        float r = params.red();
        float g = params.green();
        float b = params.blue();
        Integer dye = be.getDyeColor();
        if (dye != null) {
            r = ((dye >> 16) & 0xFF) / 255.0f;
            g = ((dye >> 8) & 0xFF) / 255.0f;
            b = (dye & 0xFF) / 255.0f;
        }
        PlumeGlow.keep(be.getBlockPos(), at, be.getLevel().getGameTime(), time * 20.0f, params.power(), r, g, b);
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

    private static float getRiseRate(Preset preset) {
        return switch (preset) {
            case ION -> 10.0f;
            default -> 14.0f;
        };
    }

    private static float getFallRate(Preset preset) {
        return switch (preset) {
            case ION -> 5.0f;
            default -> 7.0f;
        };
    }
}
