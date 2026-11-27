package dev.propulsionteam.propulsionsimulated.content.lidar;

import dev.propulsionteam.propulsionsimulated.network.LidarDebugPacket;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LidarDebug {
    public static final byte MISS = 0, BLOCK = 1, SHIP = 2, ENTITY = 3;
    private static final double VIEW_DISTANCE = 128.0;
    private static final Set<UUID> VIEWERS = ConcurrentHashMap.newKeySet();

    private LidarDebug() {}

    public static void setViewer(ServerPlayer player, boolean enabled) {
        if (enabled && player.hasPermissions(2)) VIEWERS.add(player.getUUID());
        else VIEWERS.remove(player.getUUID());
    }

    public static void forget(UUID playerId) {
        VIEWERS.remove(playerId);
    }

    public static void clear() {
        VIEWERS.clear();
    }

    @Nullable
    public static Trace start(Level level) {
        return VIEWERS.isEmpty() || !(level instanceof ServerLevel) ? null : new Trace();
    }

    public static void send(Level level, BlockPos lidar, @Nullable Trace trace) {
        if (trace == null || trace.origin == null || !(level instanceof ServerLevel server)) return;
        if (!server.getServer().isSameThread()) {
            server.getServer().execute(() -> send(level, lidar, trace));
            return;
        }
        LidarDebugPacket packet = null;
        for (ServerPlayer player : server.players()) {
            if (!VIEWERS.contains(player.getUUID()) || !player.hasPermissions(2) || player.position().distanceToSqr(trace.origin) > VIEW_DISTANCE * VIEW_DISTANCE) continue;
            if (packet == null) packet = new LidarDebugPacket(lidar.asLong(), trace.origin, trace.ends.toFloatArray(), trace.kinds.toByteArray());
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    public static final class Trace {
        @Nullable
        Vec3 origin;
        final FloatArrayList ends = new FloatArrayList();
        final ByteArrayList kinds = new ByteArrayList();

        synchronized void add(Vec3 start, double x, double y, double z, byte kind) {
            origin = start;
            ends.add((float) x);
            ends.add((float) y);
            ends.add((float) z);
            kinds.add(kind);
        }
    }
}
