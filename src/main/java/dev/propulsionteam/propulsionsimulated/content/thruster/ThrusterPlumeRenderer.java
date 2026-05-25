package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Animated translucent exhaust (Aeronautics steam-vent style).
 */
public final class ThrusterPlumeRenderer {
    private static final int FULL_BRIGHT = 15728880;
    private static final float MIN_LENGTH_SCALE = 0.65f;
    private static final float MAX_LENGTH_SCALE = 3.25f;
    /** Plume mesh base ring sits at y = 11/16 in partial space (+Y = exhaust). */
    private static final float PLUME_MESH_BASE_Y = 11f / 16f;
    private static final BlockState PLUME_BAKE_STATE = Blocks.AIR.defaultBlockState();

    private ThrusterPlumeRenderer() {
    }

    public static void render(AbstractThrusterBlockEntity blockEntity, float partialTick, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (!blockEntity.shouldRenderTexturePlume()) {
            return;
        }

        ThrusterPlumeVisuals visuals = blockEntity.getPlumeVisuals();
        int alpha = visuals.getAlpha(partialTick);
        if (alpha <= 2) {
            return;
        }

        float throttle = visuals.getLength(partialTick);
        float lengthScale = computeLengthScale(blockEntity, throttle);
        float widthScale = plumeWidthScale(blockEntity);
        int[] tint = plumeTint(blockEntity);

        Vec3 fromCenter = nozzleOffsetFromCenter(blockEntity);
        Vec3 exhaustVec = exhaustVector(blockEntity);
        boolean tilted = isTiltedExhaust(exhaustVec);

        ms.pushPose();
        ms.translate(0.5 + fromCenter.x, 0.5 + fromCenter.y, 0.5 + fromCenter.z);

        if (tilted) {
            alignPlumeYToVector(ms, exhaustVec);
        } else {
            alignPlumeYToDirection(ms, Direction.getNearest(
                    Mth.clamp((int) Math.round(exhaustVec.x), -1, 1),
                    Mth.clamp((int) Math.round(exhaustVec.y), -1, 1),
                    Mth.clamp((int) Math.round(exhaustVec.z), -1, 1)));
        }

        // Retract jet into nozzle at low throttle (Aeronautics steam vent)
        ms.translate(0.0, -(1.0f - throttle) / 3.0f, 0.0);
        // Seat plume base ring on nozzle opening, not below it
        ms.translate(0.0, -PLUME_MESH_BASE_Y, 0.0);

        ms.scale(widthScale, lengthScale, widthScale);

        renderPlumeGeometry(ms, buffer, alpha, tint[0], tint[1], tint[2]);
        ms.popPose();
    }

    private static Vec3 nozzleOffsetFromCenter(AbstractThrusterBlockEntity blockEntity) {
        if (blockEntity instanceof VectorThrusterBlockEntity
                || (blockEntity.isMultiblockThruster() && blockEntity.isMultiblockController())) {
            return blockEntity.getParticleDebugNozzlePositionLocal()
                    .subtract(Vec3.atCenterOf(blockEntity.getBlockPos()));
        }
        return ThrusterPlumeAnchors.offsetFromBlockCenter(blockEntity);
    }

    private static Vec3 exhaustVector(AbstractThrusterBlockEntity blockEntity) {
        if (blockEntity instanceof VectorThrusterBlockEntity) {
            return blockEntity.getParticleDebugExhaustDirectionLocal().normalize();
        }
        Direction exhaust = blockEntity.getFacing().getOpposite();
        return new Vec3(exhaust.getStepX(), exhaust.getStepY(), exhaust.getStepZ());
    }

    private static boolean isTiltedExhaust(Vec3 exhaustVec) {
        double ax = Math.abs(exhaustVec.x);
        double ay = Math.abs(exhaustVec.y);
        double az = Math.abs(exhaustVec.z);
        return (ax > 0.05 && ax < 0.95 && ay > 0.05 && ay < 0.95)
                || (ax > 0.05 && ax < 0.95 && az > 0.05 && az < 0.95)
                || (ay > 0.05 && ay < 0.95 && az > 0.05 && az < 0.95);
    }

    private static void renderPlumeGeometry(PoseStack ms, MultiBufferSource buffer, int alpha,
                                              int red, int green, int blue) {
        VertexConsumer translucent = buffer.getBuffer(RenderType.translucent());
        CachedBuffers.partial(PropulsionPartialModels.THRUSTER_PLUME_BASE, PLUME_BAKE_STATE)
                .disableDiffuse()
                .light(FULL_BRIGHT)
                .color(red, green, blue, alpha)
                .renderInto(ms, translucent);
        CachedBuffers.partial(PropulsionPartialModels.THRUSTER_PLUME_JET, PLUME_BAKE_STATE)
                .disableDiffuse()
                .light(FULL_BRIGHT)
                .color(red, green, blue, alpha)
                .renderInto(ms, translucent);
    }

    private static float computeLengthScale(AbstractThrusterBlockEntity blockEntity, float throttle) {
        float obstruction = blockEntity.calculateObstructionEffect();
        float scale = Mth.lerp(Mth.clamp(throttle, 0.0f, 1.0f), MIN_LENGTH_SCALE, MAX_LENGTH_SCALE);
        return scale * (0.35f + 0.65f * obstruction);
    }

    private static float plumeWidthScale(AbstractThrusterBlockEntity blockEntity) {
        int w = blockEntity.width;
        if (w <= 1) {
            return 1.0f;
        }
        return w == 2 ? 1.35f : 1.75f;
    }

    private static int[] plumeTint(AbstractThrusterBlockEntity blockEntity) {
        Integer dye = blockEntity.getDyeColor();
        if (dye != null) {
            int rgb = dye & 0xFFFFFF;
            return new int[]{(rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF};
        }
        if (blockEntity.isIon()) {
            return new int[]{180, 210, 255};
        }
        return new int[]{255, 255, 255};
    }

    /** Partial plume mesh extends along +Y; rotate +Y to world exhaust. */
    private static void alignPlumeYToDirection(PoseStack ms, Direction exhaust) {
        switch (exhaust) {
            case UP -> {
            }
            case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(180));
            case SOUTH -> ms.mulPose(Axis.XP.rotationDegrees(-90));
            case NORTH -> ms.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> ms.mulPose(Axis.ZP.rotationDegrees(-90));
            case WEST -> ms.mulPose(Axis.ZP.rotationDegrees(90));
        }
    }

    private static void alignPlumeYToVector(PoseStack ms, Vec3 exhaustDir) {
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        double dot = up.dot(exhaustDir);
        if (dot > 0.9999) {
            return;
        }
        if (dot < -0.9999) {
            ms.mulPose(Axis.XP.rotationDegrees(180.0f));
            return;
        }
        Vec3 cross = up.cross(exhaustDir);
        if (cross.lengthSqr() < 1.0e-8) {
            return;
        }
        cross = cross.normalize();
        float angle = (float) Math.acos(Mth.clamp((float) dot, -1.0f, 1.0f));
        ms.mulPose(new Quaternionf().rotationAxis(angle, (float) cross.x, (float) cross.y, (float) cross.z));
    }
}
