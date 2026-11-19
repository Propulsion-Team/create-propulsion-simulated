package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ThrusterInteractionHandler {
    public static boolean handleInteraction(BlockEntity blockEntity, Player player, InteractionHand hand) {
        return PropulsionCoreBehaviour.handleInteraction(blockEntity, player, hand)
                || SoundMufflerBehaviour.handleInteraction(blockEntity, player, hand)
                || handleDyeInteraction(blockEntity, player, hand);
    }

    private static boolean handleDyeInteraction(BlockEntity blockEntity, Player player, InteractionHand hand) {
        if (!(blockEntity instanceof AbstractThrusterBlockEntity thruster)) return false;
        ItemStack held = player.getItemInHand(hand);
        String currentDye = thruster.getDyeId();
        if (held.isEmpty()) {
            if (currentDye == null) return false;
            if (!thruster.getLevel().isClientSide()) {
                thruster.setDyeId(null);
                Block.popResource(thruster.getLevel(), thruster.getBlockPos(), new ItemStack(
                        BuiltInRegistries.ITEM.get(ResourceLocation.parse(currentDye))));
            }
            return true;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        if (!PropulsionConfig.isDyeConfigured(itemId)) return false;
        if (!thruster.getLevel().isClientSide() && !itemId.equals(currentDye)) {
            if (currentDye != null) {
                Block.popResource(thruster.getLevel(), thruster.getBlockPos(), new ItemStack(
                        BuiltInRegistries.ITEM.get(ResourceLocation.parse(currentDye))));
            }
            thruster.setDyeId(itemId);
            if (!player.isCreative()) held.shrink(1);
        }
        return true;
    }
}
