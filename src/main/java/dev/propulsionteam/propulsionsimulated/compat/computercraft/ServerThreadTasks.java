package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * ComputerCraft calls peripheral attach/detach from its computer worker threads. World work from
 * those callbacks must be queued for the server thread.
 *
 * <p>Do not use {@link MinecraftServer#execute}: once the server is stopping it runs the task
 * inline on the calling thread. On a CC worker that means world access off the server thread
 * (e.g. a redstone read that loads a chunk), which blocks until CC kills the computer for
 * running over its time limit.
 */
final class ServerThreadTasks {
    private ServerThreadTasks() {}

    /** Queues {@code task} for the server thread; dropped if the server is not running. */
    static void queue(Runnable task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isRunning()) {
            return; // shutting down: nothing left to restore
        }
        server.tell(new TickTask(server.getTickCount(), task));
    }
}
