package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.propulsionteam.propulsionsimulated.client.render.plume.ThrusterVisualEffects;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorRedstoneLinkRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterDebugRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterRenderer;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class LiquidVectorThrusterRenderer extends SmartBlockEntityRenderer<LiquidVectorThrusterBlockEntity> {

    public LiquidVectorThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(LiquidVectorThrusterBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTick, ms, buffer, light, overlay);
        ThrusterDebugRenderer.render(be, ms, buffer);

        if (be.shouldRenderShaderPlume()) {
            ThrusterVisualEffects.render(be, partialTick, ms, buffer, ThrusterVisualEffects.Preset.FIRE);
        }

        if (be == null || be.isRemoved()) return;

        BlockState state = be.getBlockState();
        PartialModel bodyModel = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_BODY;
        PartialModel flapTop = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_FLAP_TOP;
        PartialModel flapBottom = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_FLAP_BOTTOM;
        PartialModel flapLeft = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_FLAP_LEFT;
        PartialModel flapRight = PropulsionPartialModels.LIQUID_VECTOR_THRUSTER_FLAP_RIGHT;

        VectorThrusterRenderer.renderThruster(be, partialTick, ms, buffer, light, overlay, state, bodyModel, flapTop, flapBottom, flapLeft, flapRight);

        VectorRedstoneLinkRenderer.renderOnBlockEntity(be, partialTick, ms, buffer, light, overlay);
    }
}
