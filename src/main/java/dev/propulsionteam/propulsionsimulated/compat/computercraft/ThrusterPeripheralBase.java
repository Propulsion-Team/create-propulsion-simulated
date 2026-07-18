package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * ComputerCraft invokes peripheral attachment callbacks from a computer worker thread.
 * Keep those callbacks limited to local bookkeeping and defer block entity changes to
 * the server thread.
 */
public abstract class ThrusterPeripheralBase<T extends AbstractThrusterBlockEntity> implements IPeripheral {
    protected final T blockEntity;
    private final Set<IComputerAccess> attachedComputers =
        Collections.newSetFromMap(new IdentityHashMap<>());

    protected ThrusterPeripheralBase(T blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof ThrusterPeripheralBase<?> peripheral
            && blockEntity == peripheral.blockEntity;
    }

    @Override
    public final void attach(@NotNull IComputerAccess computer) {
        boolean first;
        synchronized (attachedComputers) {
            first = attachedComputers.add(computer) && attachedComputers.size() == 1;
        }
        if (first) {
            scheduleAttachmentState(true);
        }
    }

    @Override
    public final void detach(@NotNull IComputerAccess computer) {
        boolean last;
        synchronized (attachedComputers) {
            attachedComputers.remove(computer);
            last = attachedComputers.isEmpty();
        }
        if (last) {
            scheduleAttachmentState(false);
        }
    }

    private void scheduleAttachmentState(boolean attached) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> applyAttachmentState(attached));
    }

    private void applyAttachmentState(boolean requestedAttached) {
        boolean attached;
        synchronized (attachedComputers) {
            attached = !attachedComputers.isEmpty();
        }
        if (attached != requestedAttached || blockEntity.isRemoved()) {
            return;
        }
        if (blockEntity.computerBehaviour != null) {
            blockEntity.computerBehaviour.setHasAttachedComputer(attached);
        }
        if (attached) {
            return;
        }

        Level level = blockEntity.getLevel();
        blockEntity.setDigitalInput(0.0f);
        if (level != null) {
            blockEntity.setRedstoneInput(level.getBestNeighborSignal(blockEntity.getBlockPos()));
        }
        blockEntity.setControlMode(ControlMode.NORMAL);
    }
}
