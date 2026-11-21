package dev.propulsionteam.propulsionsimulated.content.thruster;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

/** Server-side record of which players reported particle plumes. Unknown players receive no plume particles until they report. */
public final class PlumeViewerPreferences {
    private static final Set<UUID> PARTICLE_VIEWERS = ConcurrentHashMap.newKeySet();

    private PlumeViewerPreferences() {}

    public static void set(ServerPlayer player, boolean shaderPlumes) {
        if (shaderPlumes) {
            PARTICLE_VIEWERS.remove(player.getUUID());
        } else {
            PARTICLE_VIEWERS.add(player.getUUID());
        }
    }

    public static boolean wantsParticles(ServerPlayer player) {
        return PARTICLE_VIEWERS.contains(player.getUUID());
    }

    public static void forget(UUID playerId) {
        PARTICLE_VIEWERS.remove(playerId);
    }

    public static void clear() {
        PARTICLE_VIEWERS.clear();
    }
}
