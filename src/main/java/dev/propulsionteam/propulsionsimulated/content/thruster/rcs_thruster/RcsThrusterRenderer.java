package dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorRedstoneLinkRenderer;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Vector3d;

public class RcsThrusterRenderer implements BlockEntityRenderer<RcsThrusterBlockEntity> {
    public RcsThrusterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(RcsThrusterBlockEntity be) {
        return be.getRenderBoundingBox();
    }

    @Override
    public void render(RcsThrusterBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        VectorRedstoneLinkRenderer.renderLinks(be, be.links, ms, buffer, light, overlay);
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(RcsOrientation.rotation(RcsThrusterBlock.normal(be.getBlockState())));
        ms.translate(-0.5, -0.5, -0.5);
        float scale = PropulsionConfig.RCS_FLAME_SCALE.get().floatValue();
        for (int i = 0; i < be.links.length; i++) {
            if (!be.isFiring(i) || be.thrust() <= 0) continue;
            Vector3d pivot = RcsOrientation.nozzle(be.isSingle(), i);
            ms.pushPose();
            ms.translate(pivot.x, pivot.y, pivot.z);
            ms.scale(scale, scale, scale);
            ms.translate(-pivot.x, -pivot.y, -pivot.z);
            var model = be.isSingle() ? PropulsionPartialModels.SINGLE_RCS_FLAME : PropulsionPartialModels.RCS_FLAMES[i];
            CachedBuffers.partial(model, be.getBlockState()).light(LightTexture.FULL_BRIGHT).overlay(overlay)
                    .renderInto(ms, buffer.getBuffer(RenderType.translucent()));
            ms.popPose();
        }
        ms.popPose();
    }
}
