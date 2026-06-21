package dev.propulsionteam.propulsionsimulated.content.thruster.liquid_vector_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.propulsionteam.propulsionsimulated.client.render.plume.ThrusterVisualEffects;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorRedstoneLinkRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterDebugRenderer;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class LiquidVectorThrusterRenderer extends SmartBlockEntityRenderer<LiquidVectorThrusterBlockEntity> {
    private static final float PIVOT_X = 7.0f / 16.0f;
    private static final float PIVOT_Y = 9.0f / 16.0f;
    private static final float PIVOT_Z = 1.0f / 16.0f;
    private static final float FLAP_PIVOT_TOP_X = 8.0f / 16.0f;
    private static final float FLAP_PIVOT_TOP_Y = 13.0f / 16.0f;
    private static final float FLAP_PIVOT_BOTTOM_Y = 3.0f / 16.0f;
    private static final float FLAP_PIVOT_LEFT_X = 3.0f / 16.0f;
    private static final float FLAP_PIVOT_RIGHT_X = 13.0f / 16.0f;
    private static final float FLAP_PIVOT_SIDE_Y = 8.0f / 16.0f;
    private static final float FLAP_PIVOT_Z = 12.0f / 16.0f;
    private static final float FLAP_ANGLE_IDLE = 22.5f;
    private static final float FLAP_ANGLE_DELTA = 30.0f;

    public LiquidVectorThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(LiquidVectorThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        VectorThrusterDebugRenderer.render(be);

        ThrusterVisualEffects.render(be, partialTicks, ms, buffer, ThrusterVisualEffects.Preset.FIRE);

        renderBody(be, partialTicks, ms, buffer, light, overlay);
        VectorRedstoneLinkRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
    }

    private static void renderBody(LiquidVectorThrusterBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be == null || be.isRemoved()) return;
        BlockState state = be.getBlockState();
        if (!state.hasProperty(AbstractThrusterBlock.FACING)) return;
        Direction facing = state.getValue(AbstractThrusterBlock.FACING);

        float xAxis = Mth.clamp(be.getInterpolatedVectorX(partialTick), -1.0f, 1.0f);
        float yAxis = Mth.clamp(be.getInterpolatedVectorY(partialTick), -1.0f, 1.0f);
        float yawDegrees = xAxis * LiquidVectorThrusterBlockEntity.MAX_VISUAL_TILT_DEGREES;
        float pitchDegrees = -yAxis * LiquidVectorThrusterBlockEntity.MAX_VISUAL_TILT_DEGREES;
        if (facing == Direction.UP) {
            yawDegrees = -yawDegrees;
            pitchDegrees = -pitchDegrees;
        }

        float flapProgress = Mth.clamp(be.getInterpolatedFlapProgress(partialTick), 0.0f, 1.0f);
        float flapAngle = FLAP_ANGLE_IDLE - flapProgress * FLAP_ANGLE_DELTA;

        PartialModel bodyModel = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_BODY;
        PartialModel flapTop = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_FLAP_TOP;
        PartialModel flapBottom = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_FLAP_BOTTOM;
        PartialModel flapLeft = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_FLAP_LEFT;
        PartialModel flapRight = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_FLAP_RIGHT;
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        applyFacingRotation(ms, facing);
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
        ms.mulPose(Axis.YP.rotationDegrees(yawDegrees));
        ms.mulPose(Axis.XP.rotationDegrees(pitchDegrees));
        ms.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
        CachedBuffers.partial(bodyModel, state).light(light).overlay(overlay).renderInto(ms, vb);
        renderFlap(ms, vb, state, flapTop, light, overlay, FLAP_PIVOT_TOP_X, FLAP_PIVOT_TOP_Y, FLAP_PIVOT_Z, Axis.XP, -flapAngle);
        renderFlap(ms, vb, state, flapBottom, light, overlay, FLAP_PIVOT_TOP_X, FLAP_PIVOT_BOTTOM_Y, FLAP_PIVOT_Z, Axis.XP, flapAngle);
        renderFlap(ms, vb, state, flapLeft, light, overlay, FLAP_PIVOT_LEFT_X, FLAP_PIVOT_SIDE_Y, FLAP_PIVOT_Z, Axis.YP, -flapAngle);
        renderFlap(ms, vb, state, flapRight, light, overlay, FLAP_PIVOT_RIGHT_X, FLAP_PIVOT_SIDE_Y, FLAP_PIVOT_Z, Axis.YP, flapAngle);
        ms.popPose();
    }

    private static void renderFlap(PoseStack ms, VertexConsumer vb, BlockState state, PartialModel model, int light, int overlay,
                                   float pivotX, float pivotY, float pivotZ, Axis axis, float angleDegrees) {
        ms.pushPose();
        ms.translate(pivotX, pivotY, pivotZ);
        ms.mulPose(axis.rotationDegrees(angleDegrees));
        ms.translate(-pivotX, -pivotY, -pivotZ);
        CachedBuffers.partial(model, state).light(light).overlay(overlay).renderInto(ms, vb);
        ms.popPose();
    }

    private static void applyFacingRotation(PoseStack ms, Direction facing) {
        switch (facing) {
            case NORTH -> { }
            case SOUTH -> ms.mulPose(Axis.YP.rotationDegrees(-180));
            case WEST -> ms.mulPose(Axis.YP.rotationDegrees(-270));
            case EAST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> ms.mulPose(Axis.XP.rotationDegrees(-270));
            case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(-90));
        }
    }

    @Override
    public boolean shouldRenderOffScreen(LiquidVectorThrusterBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
