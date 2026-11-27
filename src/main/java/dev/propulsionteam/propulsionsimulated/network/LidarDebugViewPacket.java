package dev.propulsionteam.propulsionsimulated.network;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.lidar.LidarDebug;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LidarDebugViewPacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<LidarDebugViewPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "lidar_debug_view"));
    public static final StreamCodec<ByteBuf, LidarDebugViewPacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(LidarDebugViewPacket::new, LidarDebugViewPacket::enabled);

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && (!enabled || player.hasPermissions(2))) LidarDebug.setViewer(player, enabled);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
