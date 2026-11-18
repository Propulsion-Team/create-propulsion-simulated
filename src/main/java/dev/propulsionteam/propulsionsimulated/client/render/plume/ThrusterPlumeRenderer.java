package dev.propulsionteam.propulsionsimulated.client.render.plume;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterPlumeResolver;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterPlumeSpec;
import net.minecraft.client.renderer.MultiBufferSource;

/** Renderer entrypoint shared by every thruster renderer. */
public final class ThrusterPlumeRenderer {
    private ThrusterPlumeRenderer() {}

    public static void render(AbstractThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer) {
        if (!PropulsionConfig.useShaderPlumes()) return;
        ThrusterPlumeSpec spec = ThrusterPlumeResolver.resolve(be);
        if (!spec.active()) return;
        ThrusterVisualEffects.render(be, partialTicks, ms, buffer, preset(spec.style()), spec.power());
    }

    private static ThrusterVisualEffects.Preset preset(ThrusterPlumeSpec.Style style) {
        return switch (style) {
            case FIRE -> ThrusterVisualEffects.Preset.FIRE;
            case SOLID -> ThrusterVisualEffects.Preset.SOLID;
            case SUPERHEATED_SOLID -> ThrusterVisualEffects.Preset.SUPERHEATED_SOLID;
            case ION -> ThrusterVisualEffects.Preset.ION;
            case VECTOR -> ThrusterVisualEffects.Preset.VECTOR;
            case PLASMA -> ThrusterVisualEffects.Preset.PLASMA;
        };
    }
}
