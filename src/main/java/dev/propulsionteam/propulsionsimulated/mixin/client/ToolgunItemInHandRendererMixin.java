package dev.propulsionteam.propulsionsimulated.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.propulsionteam.propulsionsimulated.content.toolgun.ToolgunLaserHandler;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ToolgunItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void createpropulsion$captureToolgunMuzzle(LivingEntity entity, ItemStack stack,
            ItemDisplayContext context, boolean leftHand, PoseStack poseStack,
            MultiBufferSource buffers, int light, CallbackInfo ci) {
        ToolgunLaserHandler.captureMuzzle(entity, stack, context, leftHand, poseStack);
    }
}
