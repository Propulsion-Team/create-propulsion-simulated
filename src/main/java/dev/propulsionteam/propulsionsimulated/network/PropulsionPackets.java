package dev.propulsionteam.propulsionsimulated.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PropulsionPackets {
    private static final String NETWORK_PROTOCOL_VERSION = "2";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_PROTOCOL_VERSION);
        registrar.playToClient(
            SyncThrusterFuelsPacket.TYPE,
            SyncThrusterFuelsPacket.STREAM_CODEC,
            (payload, context) -> payload.handle()
        );
        registrar.playToClient(
            SyncSolidThrusterFuelsPacket.TYPE,
            SyncSolidThrusterFuelsPacket.STREAM_CODEC,
            (payload, context) -> payload.handle()
        );
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if (message instanceof CustomPacketPayload payload) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static <MSG> void sendToAll(MSG message) {
        if (message instanceof CustomPacketPayload payload) {
            PacketDistributor.sendToAllPlayers(payload);
        }
    }

    public static <MSG> void sendToTracking(MSG message, LevelChunk chunk) {
        if (message instanceof CustomPacketPayload payload) {
            PacketDistributor.sendToPlayersTrackingChunk((net.minecraft.server.level.ServerLevel) chunk.getLevel(), chunk.getPos(), payload);
        }
    }

    public static <MSG> void sendToServer(MSG message) {
        if (message instanceof CustomPacketPayload payload) {
            PacketDistributor.sendToServer(payload);
        }
    }
}
