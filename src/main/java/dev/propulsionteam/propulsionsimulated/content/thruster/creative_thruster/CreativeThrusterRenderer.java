package dev.propulsionteam.propulsionsimulated.content.thruster.creative_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.propulsionteam.propulsionsimulated.client.render.plume.ThrusterVisualEffects;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterDebugRenderer;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class CreativeThrusterRenderer extends SmartBlockEntityRenderer<CreativeThrusterBlockEntity> {
    public CreativeThrusterRenderer(final BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(final CreativeThrusterBlockEntity be,
                              final float partialTicks,
                              final PoseStack ms,
                              final MultiBufferSource buffer,
                              final int light,
                              final int overlay) {
        VectorThrusterDebugRenderer.render(be);

        if (be.isController() && be.getPlumeType() != CreativeThrusterBlockEntity.PlumeType.NONE) {
            ThrusterVisualEffects.render(
                    be,
                    partialTicks,
                    ms,
                    buffer,
                    ThrusterVisualEffects.presetForCreativePlume(be.getPlumeType())
            );
        }

        final BlockState state = be.getBlockState();

        if (be.isController() && be.isMultiblock()) {
            renderMultiblock(be, ms, buffer, light, overlay, state);
            return;
        }

        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        if (!state.hasProperty(CreativeThrusterBlock.PLACEMENT_FACING)) {
            return;
        }

        final Direction facing = state.getValue(CreativeThrusterBlock.FACING);
        final Direction placementFacing = state.getValue(CreativeThrusterBlock.PLACEMENT_FACING);
        if (facing.getAxis() == placementFacing.getAxis()) {
            return;
        }

        final SuperByteBuffer bracket = CachedBuffers.partial(PropulsionPartialModels.CREATIVE_THRUSTER_BRACKET, state);
        final VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        final float angle = this.getBracketAngle(facing, placementFacing);

        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(facing.getRotation());
        ms.mulPose(Axis.XP.rotationDegrees(90));
        ms.mulPose(Axis.ZP.rotationDegrees(angle));
        ms.translate(-0.5, -0.5, -0.5);

        bracket.light(light).overlay(overlay).renderInto(ms, vb);
        ms.popPose();
    }

    private void renderMultiblock(final CreativeThrusterBlockEntity be,
                                  final PoseStack ms,
                                  final MultiBufferSource buffer,
                                  final int light,
                                  final int overlay,
                                  final BlockState state) {
        PartialModel model = getMultiblockModel(be.width);
        if (model == null) return;

        final Direction facing = state.getValue(CreativeThrusterBlock.FACING);
        final int w = be.width;
        final SuperByteBuffer mb = CachedBuffers.partial(model, state);
        final VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        ms.pushPose();
        float cx = w * 0.5f;
        ms.translate(cx, cx, cx);
        applyFacingRotation(ms, facing);
        ms.translate(-cx, -cx, -cx);
        ms.scale(w, w, w);
        mb.light(light).overlay(overlay).renderInto(ms, vb);
        ms.popPose();
    }

    private static PartialModel getMultiblockModel(int width) {
        if (width == 2) return PropulsionPartialModels.CREATIVE_THRUSTER_MULTIBLOCK_2X2X2;
        if (width == 3) return PropulsionPartialModels.CREATIVE_THRUSTER_MULTIBLOCK_3X3X3;
        return null;
    }

    private static void applyFacingRotation(PoseStack ms, Direction facing) {
        switch (facing) {
            case NORTH -> {
            }
            case SOUTH -> ms.mulPose(Axis.YP.rotationDegrees(-180));
            case WEST -> ms.mulPose(Axis.YP.rotationDegrees(-270));
            case EAST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> ms.mulPose(Axis.XP.rotationDegrees(-270));
            case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(-90));
        }
    }

    private float getBracketAngle(final Direction facing, final Direction placementFacing) {
        final Vector3f local = new Vector3f(placementFacing.step());
        local.rotate(facing.getRotation().conjugate());
        local.rotateX((float) Math.toRadians(-90));
        final double targetAngle = Math.toDegrees(Math.atan2(local.y, local.x));
        return (float) (targetAngle + 90);
    }

    @Override
    public boolean shouldRenderOffScreen(CreativeThrusterBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}


