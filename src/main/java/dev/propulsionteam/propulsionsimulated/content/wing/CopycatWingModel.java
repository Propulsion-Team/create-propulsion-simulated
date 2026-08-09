package dev.propulsionteam.propulsionsimulated.content.wing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;

public class CopycatWingModel extends CopycatModel {
    private final int width;
    private static final float PIXELS_PER_BLOCK = 16.0f;
    private static final ResourceLocation COPYCAT_BASE_ID = ResourceLocation.fromNamespaceAndPath("create", "copycat_base");

    public CopycatWingModel(BakedModel originalModel, int width) {
        super(originalModel);
        this.width = width;
    }

    public static Function<BakedModel, ? extends BakedModel> create(int width) {
        return bakedModel -> new CopycatWingModel(bakedModel, width);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState material = CopycatModel.getMaterial(data);

        // Keep copycat base on cutout so transparent pixels render correctly when no material is applied.
        if (BuiltInRegistries.BLOCK.getKey(material.getBlock()).equals(COPYCAT_BASE_ID)) {
            return ChunkRenderTypeSet.of(RenderType.cutout());
        }

        // Painted wings should render on the applied material's own layers (solid/cutout/translucent).
        return Minecraft.getInstance()
            .getBlockRenderer()
            .getBlockModel(material)
            .getRenderTypes(material, rand, ModelData.EMPTY);
    }

    @Override
    protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand, BlockState material, ModelData wrappedData, RenderType renderType) {
        // If no custom material is applied yet, render the base wing geometry directly.
        if (material.isAir() || BuiltInRegistries.BLOCK.getKey(material.getBlock()).equals(COPYCAT_BASE_ID)) {
            return originalModel.getQuads(state, side, rand, wrappedData, renderType);
        }

        //Figure out facing
        Direction facing;
        Direction.Axis axis;
        if (state.hasProperty(CopycatWingBlock.FACING)) {
            facing = state.getValue(CopycatWingBlock.FACING);
            if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) facing = facing.getOpposite();
            axis = facing.getAxis();
        } else if (state.hasProperty(SymCopycatWingBlock.AXIS)) {
            axis = state.getValue(SymCopycatWingBlock.AXIS);
            facing = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        } else {
            axis = Direction.Axis.Y;
            facing = Direction.DOWN;
        }

        //Get model and quads
        BakedModel model = getModelOf(material);
        List<BakedQuad> templateQuads = model.getQuads(material, side, rand, wrappedData, renderType);
        List<BakedQuad> quads = new ArrayList<>();

        float halfWidth = width / 2f;
        float offsetDistance = (PIXELS_PER_BLOCK - width) / (2 * PIXELS_PER_BLOCK);

        for (boolean isPositiveSide : Iterate.trueAndFalse) {

            Direction culledFace = isPositiveSide ? facing.getOpposite() : facing;
            Vec3i placementNormal = culledFace.getNormal();
            Vec3 placementOffset = new Vec3(
                placementNormal.getX() * offsetDistance,
                placementNormal.getY() * offsetDistance,
                placementNormal.getZ() * offsetDistance
            );

            float cropExtent = halfWidth / PIXELS_PER_BLOCK;
            float minX = 0, minY = 0, minZ = 0;
            float maxX = 1, maxY = 1, maxZ = 1;
            
            //Obtaion crop box
            switch (axis) {
                case X -> { if (!isPositiveSide) minX = 1 - cropExtent; else maxX = cropExtent; }
                case Y -> { if (!isPositiveSide) minY = 1 - cropExtent; else maxY = cropExtent; }
                case Z -> { if (!isPositiveSide) minZ = 1 - cropExtent; else maxZ = cropExtent; }
            }
            AABB croppingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

            for (BakedQuad quad : templateQuads) {
                if (quad.getDirection() == culledFace) {
                    continue;
                }

                quads.add(BakedQuadHelper.cloneWithCustomGeometry(quad,
                    BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), croppingBox, placementOffset)));
            }
        }
        return quads;
    }
}
