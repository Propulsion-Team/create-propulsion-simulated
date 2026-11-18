package dev.propulsionteam.propulsionsimulated.content.propulsion_core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PropulsionCoreRenderer extends CustomRenderedItemModelRenderer {
    private static final PartialModel RING1 = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "item/propulsion_core/ring1"));
    private static final PartialModel RING2 = PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "item/propulsion_core/ring2"));

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
                          ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        renderer.render(model.getOriginalModel(), light);
        float time = AnimationTickHolder.getRenderTime();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time % 40 * 9));
        renderer.render(RING1.get(), light);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(time % 30 * 12));
        renderer.render(RING2.get(), light);
        poseStack.popPose();
    }
}
