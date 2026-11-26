package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import dev.propulsionteam.propulsionsimulated.utility.redstone_link.ShipScopedLinks;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class VectorRedstoneLinkBehaviour extends BlockEntityBehaviour implements IRedstoneLinkable {

    public static final BehaviourType<VectorRedstoneLinkBehaviour> WEST_TYPE = new BehaviourType<>();
    public static final BehaviourType<VectorRedstoneLinkBehaviour> EAST_TYPE = new BehaviourType<>();
    public static final BehaviourType<VectorRedstoneLinkBehaviour> DOWN_TYPE = new BehaviourType<>();
    public static final BehaviourType<VectorRedstoneLinkBehaviour> UP_TYPE = new BehaviourType<>();

    public static final java.util.List<BehaviourType<VectorRedstoneLinkBehaviour>> RCS_TYPES = java.util.List.of(
            new BehaviourType<>(), new BehaviourType<>(), new BehaviourType<>(), new BehaviourType<>());

    private enum Mode {
        TRANSMIT, RECEIVE
    }

    private Frequency frequencyFirst;
    private Frequency frequencyLast;
    private final ValueBoxTransform firstSlot;
    private final ValueBoxTransform secondSlot;

    public boolean newPosition;
    private Mode mode;
    private IntSupplier transmission;
    private IntConsumer signalCallback;

    private final BehaviourType<VectorRedstoneLinkBehaviour> behaviourType;
    private final String sideKey;

    protected VectorRedstoneLinkBehaviour(SmartBlockEntity be, Pair<ValueBoxTransform, ValueBoxTransform> slots,
            BehaviourType<VectorRedstoneLinkBehaviour> type, String sideKey) {
        super(be);
        frequencyFirst = Frequency.EMPTY;
        frequencyLast = Frequency.EMPTY;
        firstSlot = slots.getLeft();
        secondSlot = slots.getRight();
        newPosition = true;
        behaviourType = type;
        this.sideKey = sideKey;
    }

    public static VectorRedstoneLinkBehaviour receiver(SmartBlockEntity be,
            Pair<ValueBoxTransform, ValueBoxTransform> slots,
            BehaviourType<VectorRedstoneLinkBehaviour> type, String sideKey,
            IntConsumer signalCallback) {
        VectorRedstoneLinkBehaviour behaviour = new VectorRedstoneLinkBehaviour(be, slots, type, sideKey);
        behaviour.signalCallback = signalCallback;
        behaviour.mode = Mode.RECEIVE;
        return behaviour;
    }

    public static VectorRedstoneLinkBehaviour transmitter(SmartBlockEntity be,
            Pair<ValueBoxTransform, ValueBoxTransform> slots,
            BehaviourType<VectorRedstoneLinkBehaviour> type, String sideKey,
            IntSupplier transmission) {
        VectorRedstoneLinkBehaviour behaviour = new VectorRedstoneLinkBehaviour(be, slots, type, sideKey);
        behaviour.transmission = transmission;
        behaviour.mode = Mode.TRANSMIT;
        return behaviour;
    }

    @Override
    public boolean isListening() {
        return mode == Mode.RECEIVE;
    }

    @Override
    public int getTransmittedStrength() {
        return mode == Mode.TRANSMIT ? transmission.getAsInt() : 0;
    }

    @Override
    public void setReceivedStrength(int networkPower) {
        if (!isListening())
            return;
        // Links with no frequency share the (EMPTY, EMPTY) network, so an empty transmitter
        // would reach every unset link. Unset links must ignore network power.
        boolean unset = frequencyFirst.getStack().isEmpty() && frequencyLast.getStack().isEmpty();
        signalCallback.accept(unset ? 0 : networkPower);
    }

    public void notifySignalChange() {
        Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(getWorld(), this);
        updateReceivedStrength();
    }

    private void updateReceivedStrength() {
        if (!isListening()) return;
        int power = ShipScopedLinks.powerAt(
            getWorld(), Create.REDSTONE_LINK_NETWORK_HANDLER.getNetworkOf(getWorld(), this), this, link -> false);
        setReceivedStrength(power);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (getWorld().isClientSide)
            return;
        newPosition = true;
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), this);
        if (frequencyFirst != Frequency.EMPTY || frequencyLast != Frequency.EMPTY) {
            updateReceivedStrength();
        }
    }

    @Override
    public Couple<Frequency> getNetworkKey() {
        return Couple.create(frequencyFirst, frequencyLast);
    }

    @Override
    public void unload() {
        super.unload();
        if (getWorld().isClientSide)
            return;
        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), this);
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.put(sideKey + "FrequencyFirst", frequencyFirst.getStack().saveOptional(registries));
        nbt.put(sideKey + "FrequencyLast", frequencyLast.getStack().saveOptional(registries));
        nbt.putLong(sideKey + "LastKnownPosition", blockEntity.getBlockPos().asLong());
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        long positionInTag = blockEntity.getBlockPos().asLong();
        long positionKey = nbt.getLong(sideKey + "LastKnownPosition");
        newPosition = positionInTag != positionKey;

        super.read(nbt, registries, clientPacket);
        frequencyFirst = Frequency.of(ItemStack.parseOptional(registries, nbt.getCompound(sideKey + "FrequencyFirst")));
        frequencyLast = Frequency.of(ItemStack.parseOptional(registries, nbt.getCompound(sideKey + "FrequencyLast")));
    }

    public void setFrequency(boolean first, ItemStack stack) {
        stack = stack.copy();
        stack.setCount(1);
        ItemStack toCompare = first ? frequencyFirst.getStack() : frequencyLast.getStack();
        boolean changed = !ItemStack.isSameItemSameComponents(stack, toCompare);

        if (changed)
            Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(getWorld(), this);

        if (first)
            frequencyFirst = Frequency.of(stack);
        else
            frequencyLast = Frequency.of(stack);

        if (!changed)
            return;

        blockEntity.setChanged();
        blockEntity.sendData();
        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(getWorld(), this);
        updateReceivedStrength();
    }

    @Override
    public BehaviourType<?> getType() {
        return behaviourType;
    }

    public boolean testHit(Boolean first, Vec3 hit) {
        BlockState state = blockEntity.getBlockState();
        Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
        return (first ? firstSlot : secondSlot).testHit(getWorld(), getPos(), state, localHit);
    }

    @Override
    public boolean isAlive() {
        Level level = getWorld();
        BlockPos pos = getPos();
        if (blockEntity.isChunkUnloaded())
            return false;
        if (blockEntity.isRemoved())
            return false;
        if (!level.isLoaded(pos))
            return false;
        return level.getBlockEntity(pos) == blockEntity;
    }

    @Override
    public BlockPos getLocation() {
        return getPos();
    }

    public ValueBoxTransform getFirstSlot() {
        return firstSlot;
    }

    public ValueBoxTransform getSecondSlot() {
        return secondSlot;
    }

    public Frequency getFrequency(boolean first) {
        return first ? frequencyFirst : frequencyLast;
    }
}
