package dev.propulsionteam.propulsionsimulated.network;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.lidar.LidarMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LidarSettingsPacket(int detection, boolean exclude, boolean binary, boolean inverted, int range) implements CustomPacketPayload {
    public static final Type<LidarSettingsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "lidar_settings"));
    public static final StreamCodec<ByteBuf, LidarSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LidarSettingsPacket::detection,
            ByteBufCodecs.BOOL, LidarSettingsPacket::exclude,
            ByteBufCodecs.BOOL, LidarSettingsPacket::binary,
            ByteBufCodecs.BOOL, LidarSettingsPacket::inverted,
            ByteBufCodecs.VAR_INT, LidarSettingsPacket::range,
            LidarSettingsPacket::new);

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof LidarMenu menu
                && menu.contentHolder != null && menu.stillValid(player))
                menu.contentHolder.applySettings(detection, exclude, binary, inverted, range);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
