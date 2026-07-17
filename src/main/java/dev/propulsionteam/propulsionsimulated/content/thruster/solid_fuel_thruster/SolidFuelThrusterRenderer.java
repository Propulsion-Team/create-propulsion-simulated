package dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.propulsionteam.propulsionsimulated.client.render.plume.ThrusterVisualEffects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class SolidFuelThrusterRenderer extends SmartBlockEntityRenderer<SolidFuelThrusterBlockEntity> {
    public SolidFuelThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SolidFuelThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        float targetPower = be.getBurnTime() > 0 && be.validFuel() && be.getPower() > 0.0f
                ? be.getPower()
                : 0.0f;

        if (be.shouldRenderShaderPlume()) {
            ThrusterVisualEffects.render(
                    be,
                    partialTicks,
                    ms,
                    buffer,
                    be.isSuperHeated()
                            ? ThrusterVisualEffects.Preset.SUPERHEATED_SOLID
                            : ThrusterVisualEffects.Preset.SOLID,
                    targetPower
            );
        }
    }

    @Override
    public boolean shouldRenderOffScreen(SolidFuelThrusterBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
