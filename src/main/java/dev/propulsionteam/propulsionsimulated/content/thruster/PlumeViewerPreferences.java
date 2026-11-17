package dev.propulsionteam.propulsionsimulated.content.thruster;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

/** Server-side record of which players render plumes with the shader and therefore must not receive plume particles. */
public final class PlumeViewerPreferences {
    private static final Set<UUID> SHADER_VIEWERS = ConcurrentHashMap.newKeySet();

    private PlumeViewerPreferences() {}

    public static void set(ServerPlayer player, boolean shaderPlumes) {
        if (shaderPlumes) {
            SHADER_VIEWERS.add(player.getUUID());
        } else {
            SHADER_VIEWERS.remove(player.getUUID());
        }
    }

    public static boolean wantsParticles(ServerPlayer player) {
        return !SHADER_VIEWERS.contains(player.getUUID());
    }

    public static void forget(UUID playerId) {
        SHADER_VIEWERS.remove(playerId);
    }

    public static void clear() {
        SHADER_VIEWERS.clear();
    }
}
