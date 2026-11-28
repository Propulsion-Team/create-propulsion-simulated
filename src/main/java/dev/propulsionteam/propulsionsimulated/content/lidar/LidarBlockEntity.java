package dev.propulsionteam.propulsionsimulated.content.lidar;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.CCProxy;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.ryanhcode.sable.Sable;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class LidarBlockEntity extends SmartBlockEntity implements MenuProvider {
    public static final int FILTER_SLOTS = 18;
    public static final int MAX_RANGE = (int) LidarRaycast.MAX_RANGE;
    public static final int DEFAULT_RANGE = 32;

    public AbstractComputerBehaviour computerBehaviour;

    private int detection = LidarDetection.DEFAULTS;
    private boolean exclude;
    private boolean binary;
    private boolean inverted;
    private int range = DEFAULT_RANGE;
    private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS);
    private int signal;

    @Nullable
    private List<FilterItemStack> filterStacks;
    private final Map<Block, Boolean> filterCache = new IdentityHashMap<>();

    public LidarBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.LIDAR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = CCProxy.behaviour(this));
        }
    }

    public Direction getFacing() {
        return getBlockState().getValue(LidarBlock.FACING);
    }

    @Nullable
    public LidarRaycast.Hit raycast(LidarRaycast.Options options) {
        if (level == null) return null;
        LidarDebug.Trace trace = LidarDebug.start(level);
        LidarRaycast.Hit hit = LidarRaycast.cast(level, worldPosition, getFacing(), options, trace);
        LidarDebug.send(level, worldPosition, trace);
        return hit;
    }

    public CompletableFuture<List<LidarRaycast.Hit>> scan(LidarRaycast.Options options, double spreadRadians, int rays) {
        if (level == null) return CompletableFuture.completedFuture(List.of());
        Level level = this.level;
        BlockPos pos = worldPosition;
        LidarDebug.Trace trace = LidarDebug.start(level);
        return LidarRaycast.scan(level, pos, getFacing(), options, spreadRadians, rays, trace)
            .whenComplete((hits, error) -> LidarDebug.send(level, pos, trace));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        setSignal(computeSignal());
    }

    private int computeSignal() {
        boolean anything = LidarDetection.BLOCKS.in(detection) || LidarDetection.ENTITIES.in(detection)
            || LidarDetection.PLAYERS.in(detection);
        LidarRaycast.Hit hit = anything && range > 0 ? raycast(redstoneOptions()) : null;
        int strength;
        if (hit == null) strength = 0;
        else if (binary) strength = 15;
        else strength = Mth.clamp(15 - (int) (15 * hit.distance() / range), 1, 15);
        return inverted ? 15 - strength : strength;
    }

    private LidarRaycast.Options redstoneOptions() {
        Predicate<BlockState> matches = hasFilter() ? this::matchesFilter : null;
        return new LidarRaycast.Options(range, null,
            LidarDetection.FLUIDS.in(detection), LidarDetection.NON_SOLID.in(detection),
            LidarDetection.SEE_THROUGH.in(detection), LidarDetection.ENTITIES.in(detection),
            LidarDetection.PLAYERS.in(detection), LidarDetection.SHIPS.in(detection),
            LidarDetection.OWN_SHIP.in(detection), exclude ? null : matches, exclude ? matches : null,
            LidarDetection.BLOCKS.in(detection), LidarDetection.PRECISE.in(detection));
    }

    private boolean hasFilter() {
        return !filterStacks().isEmpty();
    }

    private List<FilterItemStack> filterStacks() {
        if (filterStacks == null) {
            List<FilterItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < filter.getSlots(); i++) {
                ItemStack stack = filter.getStackInSlot(i);
                if (!stack.isEmpty()) stacks.add(FilterItemStack.of(stack));
            }
            filterStacks = stacks;
        }
        return filterStacks;
    }

    private boolean matchesFilter(BlockState state) {
        return filterCache.computeIfAbsent(state.getBlock(), block -> {
            FluidState fluid = state.getFluidState();
            ItemStack item = new ItemStack(block);
            for (FilterItemStack stack : filterStacks()) {
                if (!item.isEmpty() && stack.test(level, item)) return true;
                if (!fluid.isEmpty() && stack.test(level, new FluidStack(fluid.getType(), 1000))) return true;
            }
            return false;
        });
    }

    private void setSignal(int value) {
        if (value == signal) return;
        signal = value;
        setChanged();
        if (level != null) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    public int getSignal() {
        return signal;
    }

    public int getDetection() {
        return detection;
    }

    public boolean isExclude() {
        return exclude;
    }

    public boolean isBinary() {
        return binary;
    }

    public boolean isInverted() {
        return inverted;
    }

    public ItemStack getFilterStack(int slot) {
        return filter.getStackInSlot(slot);
    }

    public int getRange() {
        return range;
    }

    public void applySettings(int detection, boolean exclude, boolean binary, boolean inverted, int range) {
        this.range = Mth.clamp(range, 0, MAX_RANGE);
        this.detection = detection & LidarDetection.ALL;
        this.exclude = exclude;
        this.binary = binary;
        this.inverted = inverted;
        notifyUpdate();
    }

    public void setFilterStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= FILTER_SLOTS || ItemStack.matches(filter.getStackInSlot(slot), stack)) return;
        filter.setStackInSlot(slot, stack.copyWithCount(1));
        filterStacks = null;
        filterCache.clear();
        notifyUpdate();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Detection", detection);
        tag.putBoolean("Exclude", exclude);
        tag.putBoolean("Binary", binary);
        tag.putBoolean("Inverted", inverted);
        tag.putInt("Range", range);
        tag.putInt("Signal", signal);
        tag.put("Filter", filter.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        detection = tag.contains("Detection") ? tag.getInt("Detection") & LidarDetection.ALL : LidarDetection.DEFAULTS;
        exclude = tag.getBoolean("Exclude");
        binary = tag.getBoolean("Binary");
        inverted = tag.getBoolean("Inverted");
        range = tag.contains("Range") ? Mth.clamp(tag.getInt("Range"), 0, MAX_RANGE) : DEFAULT_RANGE;
        signal = Mth.clamp(tag.getInt("Signal"), 0, 15);
        if (tag.contains("Filter")) {
            ItemStackHandler loaded = new ItemStackHandler(FILTER_SLOTS);
            loaded.deserializeNBT(registries, tag.getCompound("Filter"));
            for (int i = 0; i < FILTER_SLOTS; i++)
                filter.setStackInSlot(i, i < loaded.getSlots() ? loaded.getStackInSlot(i) : ItemStack.EMPTY);
        }
        filterStacks = null;
        filterCache.clear();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return LidarMenu.create(id, inventory, this);
    }

    public boolean canPlayerUse(Player player) {
        return level != null && !isRemoved() && level.getBlockEntity(worldPosition) == this
            && player.position().distanceToSqr(Sable.HELPER.projectOutOfSubLevel(level, VecHelper.getCenterOf(worldPosition))) <= 64.0;
    }
}
