package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.propulsion_core.VectorCoreState;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsThrusterBlockEntity;
import net.minecraft.server.level.ServerLevel;
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

import java.util.concurrent.atomic.AtomicInteger;

public class PropulsionCoreBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<PropulsionCoreBehaviour> TYPE = new BehaviourType<>();

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(PropulsionItems.VECTOR_CORE.get());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            invalidateActiveCache();
        }
    };

    private static final AtomicInteger STATE_GENERATION = new AtomicInteger();

    private double wear;
    private long activeCacheTime = Long.MIN_VALUE;
    private int activeCacheGeneration;
    private boolean activeCacheValue;

    public PropulsionCoreBehaviour(SmartBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerLevel level)) return;
        ItemStack core = inventory.getStackInSlot(0);
        if (core.isEmpty()) return;
        if (VectorCoreState.syncDepleted(core, level.getGameTime())) {
            invalidateActiveCache();
            blockEntity.notifyUpdate();
        }
        if (!VectorCoreState.isWorking(core)) return;
        BlockEntity source = blockEntity;
        if (blockEntity instanceof AbstractThrusterBlockEntity thruster && thruster.isMultiblock()) {
            source = thruster.getControllerBE();
        }
        if (source == null || findInstalled(source) != this) return;
        double thrustPn = producedThrustPn(source);
        if (thrustPn <= 0.0d) return;
        wear += Math.pow(thrustPn / PropulsionConfig.VECTOR_CORE_OUTPUT_PN.get(), 0.7d) / 20.0d;
        int damage = (int) wear;
        blockEntity.setChanged();
        if (damage <= 0) return;
        wear -= damage;
        core.hurtAndBreak(damage, level, null, item -> {
        });
        invalidateActiveCache();
    }

    private static void invalidateActiveCache() {
        STATE_GENERATION.incrementAndGet();
    }

    private static double producedThrustPn(BlockEntity source) {
        if (source instanceof RcsThrusterBlockEntity rcs) return rcs.currentThrust();
        if (source instanceof AbstractThrusterBlockEntity thruster && thruster.canApplyThrust()) {
            return thruster.getCurrentThrust() / thruster.getThrustUnitsPerPn();
        }
        return 0.0d;
    }

    public IItemHandlerModifiable getItemHandler() {
        return inventory;
    }

    public static boolean isInstalled(BlockEntity blockEntity) {
        return findInstalled(blockEntity) != null;
    }

    public static boolean isActive(BlockEntity blockEntity) {
        PropulsionCoreBehaviour installed = findInstalled(blockEntity);
        return installed != null && VectorCoreState.isWorking(installed.inventory.getStackInSlot(0));
    }

    public static boolean isActiveCached(BlockEntity blockEntity) {
        PropulsionCoreBehaviour behaviour = get(blockEntity, TYPE);
        if (behaviour == null || !(blockEntity.getLevel() instanceof ServerLevel level)
            || !level.getServer().isSameThread()) return isActive(blockEntity);
        long time = level.getGameTime();
        int generation = STATE_GENERATION.get();
        if (behaviour.activeCacheTime != time || behaviour.activeCacheGeneration != generation) {
            behaviour.activeCacheValue = isActive(blockEntity);
            behaviour.activeCacheTime = time;
            behaviour.activeCacheGeneration = generation;
        }
        return behaviour.activeCacheValue;
    }

    public static boolean setEnabled(BlockEntity blockEntity, boolean enabled) {
        PropulsionCoreBehaviour installed = findInstalled(blockEntity);
        if (installed == null || VectorCoreState.isDepleted(installed.inventory.getStackInSlot(0))) return false;
        VectorCoreState.setEnabled(installed.inventory.getStackInSlot(0), enabled, installed.getWorld().getGameTime());
        invalidateActiveCache();
        installed.blockEntity.notifyUpdate();
        return true;
    }

    public static boolean toggle(BlockEntity blockEntity) {
        setEnabled(blockEntity, !isActive(blockEntity));
        return isActive(blockEntity);
    }

    private static PropulsionCoreBehaviour findInstalled(BlockEntity blockEntity) {
        PropulsionCoreBehaviour behaviour = get(blockEntity, TYPE);
        if (behaviour == null) return null;
        if (!behaviour.inventory.getStackInSlot(0).isEmpty()) return behaviour;
        if (!(blockEntity instanceof AbstractThrusterBlockEntity thruster) || !thruster.isMultiblock()) return null;
        AbstractThrusterBlockEntity controller = thruster.getControllerBE();
        if (controller == null) return null;
        BlockPos origin = controller.getBlockPos();
        int size = controller.width;
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
            BlockEntity member = SimulatedThrustAdapter.getBlockEntitySafe(blockEntity.getLevel(), pos);
            if (!(member instanceof AbstractThrusterBlockEntity other) || other.getControllerBE() != controller) continue;
            PropulsionCoreBehaviour installed = get(member, TYPE);
            if (installed != null && !installed.inventory.getStackInSlot(0).isEmpty()) return installed;
        }
        return null;
    }

    public static boolean handleInteraction(BlockEntity blockEntity, Player player, InteractionHand hand) {
        PropulsionCoreBehaviour behaviour = get(blockEntity, TYPE);
        if (behaviour == null || player.isSpectator() || !player.mayBuild()) return false;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            PropulsionCoreBehaviour installed = findInstalled(blockEntity);
            if (installed == null) return false;
            if (!behaviour.getWorld().isClientSide) {
                ItemStack removed = installed.inventory.getStackInSlot(0);
                installed.inventory.setStackInSlot(0, ItemStack.EMPTY);
                installed.blockEntity.notifyUpdate();
                player.getInventory().placeItemBackInInventory(removed);
            }
            return true;
        }
        if (!held.is(PropulsionItems.VECTOR_CORE.get())) return false;
        if (!behaviour.getWorld().isClientSide && !isInstalled(blockEntity)) {
            behaviour.inventory.setStackInSlot(0, held.copyWithCount(1));
            if (!player.isCreative()) held.shrink(1);
            behaviour.blockEntity.notifyUpdate();
        }
        return true;
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        ItemStack core = inventory.getStackInSlot(0);
        if (!core.isEmpty()) tag.put("PropulsionCore", core.save(registries));
        tag.putDouble("PropulsionCoreWear", wear);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        inventory.setStackInSlot(0, tag.contains("PropulsionCore")
            ? ItemStack.parseOptional(registries, tag.getCompound("PropulsionCore")) : ItemStack.EMPTY);
        wear = tag.getDouble("PropulsionCoreWear");
    }

    /** Empties the slot without dropping it, for moves that already copied the item to the new block entity. */
    public void clearContent() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    public void destroy() {
        if (getWorld() == null || getWorld().isClientSide) return;
        ItemStack core = inventory.getStackInSlot(0);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        Block.popResource(getWorld(), getPos(), core);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }
}
