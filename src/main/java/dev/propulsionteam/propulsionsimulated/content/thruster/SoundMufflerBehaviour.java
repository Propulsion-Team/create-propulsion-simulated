package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.Function;

public class SoundMufflerBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<SoundMufflerBehaviour> TYPE = new BehaviourType<>();
    public static final float MUFFLED_VOLUME = 0.3f;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(PropulsionItems.SOUND_MUFFLER.get());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    };

    public SoundMufflerBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }

    public IItemHandlerModifiable getItemHandler() {
        return inventory;
    }

    public static float getVolumeMultiplier(BlockEntity blockEntity) {
        return getVolumeMultiplier(blockEntity,
                pos -> SimulatedThrustAdapter.getBlockEntitySafe(blockEntity.getLevel(), pos));
    }

    static float getVolumeMultiplier(BlockEntity blockEntity, Function<BlockPos, BlockEntity> memberLookup) {
        SoundMufflerBehaviour behaviour = get(blockEntity, TYPE);
        return behaviour != null && behaviour.findInstalled(memberLookup) != null ? MUFFLED_VOLUME : 1.0f;
    }

    public static boolean handleInteraction(BlockEntity blockEntity, Player player, InteractionHand hand) {
        SoundMufflerBehaviour behaviour = get(blockEntity, TYPE);
        if (behaviour == null || player.isSpectator() || !player.mayBuild()) return false;
        ItemStack held = player.getItemInHand(hand);
        SoundMufflerBehaviour installed = behaviour.findInstalled();
        if (held.is(PropulsionItems.SOUND_MUFFLER.get())) {
            if (!behaviour.getWorld().isClientSide && behaviour.inventory.getStackInSlot(0).isEmpty()) {
                behaviour.inventory.setStackInSlot(0, held.copyWithCount(1));
                if (!player.isCreative()) held.shrink(1);
                behaviour.blockEntity.notifyUpdate();
            }
            return true;
        }
        if (held.isEmpty() && installed != null) {
            if (!behaviour.getWorld().isClientSide) {
                ItemStack removed = installed.inventory.getStackInSlot(0);
                installed.inventory.setStackInSlot(0, ItemStack.EMPTY);
                installed.blockEntity.notifyUpdate();
                player.getInventory().placeItemBackInInventory(removed);
            }
            return true;
        }
        return false;
    }

    private SoundMufflerBehaviour findInstalled() {
        return findInstalled(pos -> SimulatedThrustAdapter.getBlockEntitySafe(getWorld(), pos));
    }

    private SoundMufflerBehaviour findInstalled(Function<BlockPos, BlockEntity> memberLookup) {
        if (!inventory.getStackInSlot(0).isEmpty()) return this;
        if (!(blockEntity instanceof AbstractThrusterBlockEntity thruster) || !thruster.isMultiblock()) return null;
        AbstractThrusterBlockEntity controller = thruster.getControllerBE();
        if (controller == null) return null;
        BlockPos origin = controller.getBlockPos();
        int size = controller.width;
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
            BlockEntity member = memberLookup.apply(pos);
            if (!(member instanceof AbstractThrusterBlockEntity other) || other.getControllerBE() != controller) continue;
            SoundMufflerBehaviour behaviour = get(member, TYPE);
            if (behaviour != null && !behaviour.inventory.getStackInSlot(0).isEmpty()) return behaviour;
        }
        return null;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.put("SoundMuffler", inventory.serializeNBT(registries));
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (tag.contains("SoundMuffler")) {
            inventory.deserializeNBT(registries, tag.getCompound("SoundMuffler"));
        } else {
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Override
    public void destroy() {
        if (getWorld() == null || getWorld().isClientSide) return;
        ItemStack stack = inventory.getStackInSlot(0);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        Block.popResource(getWorld(), getPos(), stack);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
