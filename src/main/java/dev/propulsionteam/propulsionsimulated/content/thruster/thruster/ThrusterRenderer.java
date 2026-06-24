package dev.propulsionteam.propulsionsimulated.content.thruster.thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.MeshedThrusterFlameUtils;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterDebugRenderer;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ThrusterRenderer extends SmartBlockEntityRenderer<ThrusterBlockEntity> {
    public ThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ThrusterBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ThrusterDebugRenderer.render(be, ms, buffer);
        if (be.isMultiblock()) {
            if (be.isController()) {
                PartialModel model = getMultiblockModel(be.width);
                if (model == null) return;

                BlockState state = be.getBlockState();
                Direction facing = state.getValue(AbstractThrusterBlock.FACING);
                int w = be.width;

                SuperByteBuffer mb = CachedBuffers.partial(model, state);
                VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

                ms.pushPose();
                float cx = w * 0.5f;
                ms.translate(cx, cx, cx);
                applyFacingRotation(ms, facing);
                ms.translate(-cx, -cx, -cx);
                ms.scale(w, w, w);
                mb.light(light).overlay(overlay).renderInto(ms, vb);
                ms.popPose();
                MeshedThrusterFlameUtils.renderMultiblockFlame(be, partialTick, ms, buffer, w);
            }
        } else {
            MeshedThrusterFlameUtils.renderMeshFlame(be, partialTick, ms, buffer);
        }
    }

    private static PartialModel getMultiblockModel(int width) {
        if (width == 2) return PropulsionPartialModels.THRUSTER_MULTIBLOCK_2X2X2;
        if (width == 3) return PropulsionPartialModels.THRUSTER_MULTIBLOCK_3X3X3;
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
}
