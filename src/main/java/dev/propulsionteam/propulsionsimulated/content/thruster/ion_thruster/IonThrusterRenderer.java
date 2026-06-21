package dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.propulsionteam.propulsionsimulated.client.render.plume.ThrusterVisualEffects;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorRedstoneLinkRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterDebugRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class IonThrusterRenderer extends SmartBlockEntityRenderer<IonThrusterBlockEntity> {

    public IonThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(IonThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        VectorThrusterDebugRenderer.render(be);

        if (be.isController()) {
            if (be instanceof dev.propulsionteam.propulsionsimulated.content.thruster.creative_vector_thruster.CreativeVectorThrusterBlockEntity creativeVector) {
                if (creativeVector.getPlumeType() != dev.propulsionteam.propulsionsimulated.content.thruster.creative_thruster.CreativeThrusterBlockEntity.PlumeType.NONE) {
                    ThrusterVisualEffects.render(
                            be,
                            partialTicks,
                            ms,
                            buffer,
                            ThrusterVisualEffects.presetForCreativePlume(creativeVector.getPlumeType())
                    );
                }
            } else if (be instanceof dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity) {
                ThrusterVisualEffects.render(be, partialTicks, ms, buffer, ThrusterVisualEffects.Preset.VECTOR);
            } else {
                ThrusterVisualEffects.render(be, partialTicks, ms, buffer, ThrusterVisualEffects.Preset.ION);
            }
        }

        if (be.isController() && be.isMultiblock()) {
            renderMultiblock(be, ms, buffer, light, overlay);
        }

        if (be instanceof VectorThrusterBlockEntity vector) {
            VectorThrusterRenderer.render(vector, partialTicks, ms, buffer, light, overlay);
            VectorRedstoneLinkRenderer.renderOnBlockEntity(vector, partialTicks, ms, buffer, light, overlay);
        }
    }

    private static void renderMultiblock(IonThrusterBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
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
    }

    private static PartialModel getMultiblockModel(int width) {
        if (width == 2) return PropulsionPartialModels.ION_THRUSTER_MULTIBLOCK_2X2X2;
        if (width == 3) return PropulsionPartialModels.ION_THRUSTER_MULTIBLOCK_3X3X3;
        return null;
    }

    private static void applyFacingRotation(PoseStack ms, Direction facing) {
        switch (facing) {
            case UP -> {
            }
            case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(180));
            // Keep Z-axis orientations aligned with logical IO (input=facing, exhaust=facing.opposite).
            case NORTH -> {
                ms.mulPose(Axis.YP.rotationDegrees(180));
                ms.mulPose(Axis.XP.rotationDegrees(90));
            }
            case SOUTH -> ms.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> {
                ms.mulPose(Axis.YP.rotationDegrees(90));
                ms.mulPose(Axis.XP.rotationDegrees(90));
            }
            case WEST -> {
                ms.mulPose(Axis.YP.rotationDegrees(270));
                ms.mulPose(Axis.XP.rotationDegrees(90));
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(IonThrusterBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
