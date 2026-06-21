package dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.MeshedThrusterFlameUtils;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterDebugRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SolidFuelThrusterRenderer extends SmartBlockEntityRenderer<SolidFuelThrusterBlockEntity> {
    public SolidFuelThrusterRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SolidFuelThrusterBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ThrusterDebugRenderer.render(be, ms, buffer);
        MeshedThrusterFlameUtils.renderMeshFlame(be, partialTick, ms, buffer, false);
    }

}
