package dev.propulsionteam.propulsionsimulated.content.propulsion_core;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class VectorCoreItem extends Item {
    private static final ResourceLocation REPAIR_ITEM = ResourceLocation.fromNamespaceAndPath("simulated", "gyroscopic_mechanism");

    public VectorCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !VectorCoreState.syncDepleted(stack, level.getGameTime())) return;
        if (!VectorCoreState.isDepleted(stack) && entity instanceof ServerPlayer player) {
            player.playNotifySound(PropulsionSoundEvents.VECTOR_CORE_ENABLE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        return Math.min(amount, Math.max(0, stack.getMaxDamage() - 1 - stack.getDamageValue()));
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.UNBREAKING) || enchantment.is(Enchantments.MENDING);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 10;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return BuiltInRegistries.ITEM.getKey(repairCandidate.getItem()).equals(REPAIR_ITEM);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        boolean enabled = VectorCoreState.isWorking(stack);
        Component value = Component.translatable(getDescriptionId() + (enabled ? ".enabled" : ".disabled"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        tooltip.add(Component.translatable(getDescriptionId() + ".status", value).withStyle(ChatFormatting.WHITE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || VectorCoreState.isDepleted(stack)) return InteractionResultHolder.pass(stack);
        boolean enable = !VectorCoreState.isEnabled(stack);
        VectorCoreState.setEnabled(stack, enable, level.getGameTime());
        player.getCooldowns().addCooldown(this, VectorCoreState.FADE_TICKS);
        if (level.isClientSide()) {
            player.playSound((enable ? PropulsionSoundEvents.VECTOR_CORE_ENABLE
                    : PropulsionSoundEvents.VECTOR_CORE_DISABLE).get(), 1.0f, 1.0f);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }
}
