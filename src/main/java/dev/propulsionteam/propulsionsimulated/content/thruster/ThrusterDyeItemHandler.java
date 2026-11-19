package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ThrusterDyeItemHandler extends ItemStackHandler {
    private final AbstractThrusterBlockEntity thruster;

    public ThrusterDyeItemHandler(AbstractThrusterBlockEntity thruster) {
        super(1);
        this.thruster = thruster;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        String dyeId = thruster.getDyeId();
        return dyeId == null ? ItemStack.EMPTY
                : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(dyeId)));
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        thruster.setDyeId(stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return PropulsionConfig.isDyeConfigured(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }
}
