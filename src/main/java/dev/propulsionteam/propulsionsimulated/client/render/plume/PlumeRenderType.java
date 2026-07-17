package dev.propulsionteam.propulsionsimulated.client.render.plume;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class PlumeRenderType extends RenderType {
    private static final RenderType PLUME = RenderType.create(
            "createpropulsion_plume",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            VertexFormat.Mode.QUADS,
            4096,
            false,
            true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(PlumeShaders::plume))
                    .setTransparencyState(new TransparencyStateShard(
                            "createpropulsion_plume_blend",
                            () -> {
                                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                                com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                                        GlStateManager.SourceFactor.SRC_ALPHA,
                                        GlStateManager.DestFactor.ONE,
                                        GlStateManager.SourceFactor.ONE,
                                        GlStateManager.DestFactor.ONE
                                );
                            },
                            () -> {
                                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                            }
                    ))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOverlayState(RenderStateShard.NO_OVERLAY)
                    .createCompositeState(false)
    );

    private PlumeRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType plume() {
        return PLUME;
    }
}