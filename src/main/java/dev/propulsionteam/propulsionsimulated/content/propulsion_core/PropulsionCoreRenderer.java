package dev.propulsionteam.propulsionsimulated.content.propulsion_core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PropulsionCoreRenderer extends CustomRenderedItemModelRenderer {
    private static final PartialModel RING1 = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "item/propulsion_core/ring1"));
    private static final PartialModel RING2 = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "item/propulsion_core/ring2"));
    private static final PartialModel DEPLETED_CORE = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "item/propulsion_core/depleted_core"));
    private static final PartialModel DEPLETED_RING1 = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "item/propulsion_core/depleted_ring1"));
    private static final PartialModel DEPLETED_RING2 = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "item/propulsion_core/depleted_ring2"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
                          ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Level level = Minecraft.getInstance().level;
        double time = level == null ? AnimationTickHolder.getRenderTime()
                : level.getGameTime() + AnimationTickHolder.getPartialTicks();
        float phase = VectorCoreState.phaseAt(stack, time);
        float flash = VectorCoreState.flashAt(stack, time);
        boolean depleted = VectorCoreState.showsDepleted(stack);

        RenderType renderType = Sheets.translucentCullBlockSheet();
        if (flash > 0) {
            renderType = VectorCoreFlashRenderType.INSTANCE;
            renderer = PartialItemModelRenderer.of(stack, context, poseStack, buffer,
                    OverlayTexture.pack(OverlayTexture.u(flash), OverlayTexture.v(false)));
        }

        renderer.render(depleted ? DEPLETED_CORE.get() : model.getOriginalModel(), renderType, light);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(phase * 9));
        renderer.render((depleted ? DEPLETED_RING1 : RING1).get(), renderType, light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(phase * 12));
        renderer.render((depleted ? DEPLETED_RING2 : RING2).get(), renderType, light);
        poseStack.popPose();
    }
}
