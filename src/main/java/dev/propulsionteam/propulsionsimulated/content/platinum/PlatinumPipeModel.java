package dev.propulsionteam.propulsionsimulated.content.platinum;

import com.simibubi.create.content.fluids.PipeAttachmentModel;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class PlatinumPipeModel extends PipeAttachmentModel {
    private final Map<BakedQuad, BakedQuad> retextured = new IdentityHashMap<>();

    public PlatinumPipeModel(BakedModel template) {
        super(template, true);
    }

    private static TextureAtlasSprite sprite(String path) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
            .apply(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "block/" + path));
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return sprite("platinum_pipe");
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return sprite("platinum_pipe");
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        if (state.getBlock() instanceof GlassPlatinumPipeBlock) {
            return ChunkRenderTypeSet.of(RenderType.translucent());
        }
        return super.getRenderTypes(state, rand, data);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        if (state != null && state.getBlock() instanceof GlassPlatinumPipeBlock) {
            if (renderType != null && renderType != RenderType.translucent()) {
                return List.of();
            }
            renderType = null;
        }
        List<BakedQuad> quads = new ArrayList<>();
        for (BakedQuad quad : super.getQuads(state, side, rand, data, renderType)) {
            quads.add(retextured.computeIfAbsent(quad, this::retexture));
        }
        return quads;
    }

    private BakedQuad retexture(BakedQuad quad) {
        TextureAtlasSprite original = quad.getSprite();
        String replacement = switch (original.contents().name().toString()) {
            case "create:block/pipes" -> "platinum_pipe";
            case "create:block/pipes_connected" -> "platinum_pipe_connected";
            case "create:block/glass_fluid_pipe" -> "glass_platinum_pipe";
            default -> null;
        };
        if (replacement == null) {
            return quad;
        }
        TextureAtlasSprite target = sprite(replacement);
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);
            vertices[offset + 4] = Float.floatToRawIntBits(target.getU0()
                + (u - original.getU0()) / (original.getU1() - original.getU0()) * (target.getU1() - target.getU0()));
            vertices[offset + 5] = Float.floatToRawIntBits(target.getV0()
                + (v - original.getV0()) / (original.getV1() - original.getV0()) * (target.getV1() - target.getV0()));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), target, quad.isShade(), quad.hasAmbientOcclusion());
    }
}
