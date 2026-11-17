package dev.propulsionteam.propulsionsimulated.network;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.thruster.PlumeViewerPreferences;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: which plume render mode this player uses, so the server only sends plume particles to particle-mode players. */
public record PlumeRenderModePacket(boolean shaderPlumes) implements CustomPacketPayload {
    public static final Type<PlumeRenderModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "plume_render_mode"));
    public static final StreamCodec<ByteBuf, PlumeRenderModePacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(PlumeRenderModePacket::new, PlumeRenderModePacket::shaderPlumes);

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PlumeViewerPreferences.set(player, shaderPlumes);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
