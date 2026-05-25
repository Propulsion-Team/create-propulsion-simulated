package dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterPlumeRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterDebugRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class SolidFuelThrusterRenderer extends SmartBlockEntityRenderer<SolidFuelThrusterBlockEntity> {
    public SolidFuelThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SolidFuelThrusterBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        VectorThrusterDebugRenderer.render(be);
        ThrusterPlumeRenderer.render(be, partialTicks, ms, buffer, light, overlay);
    }
}
