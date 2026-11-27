package dev.propulsionteam.propulsionsimulated.network;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.client.LidarDebugRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LidarDebugPacket(long lidar, Vec3 origin, float[] ends, byte[] kinds) implements CustomPacketPayload {
    public static final Type<LidarDebugPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "lidar_debug"));
    public static final StreamCodec<FriendlyByteBuf, LidarDebugPacket> STREAM_CODEC =
            StreamCodec.ofMember(LidarDebugPacket::write, LidarDebugPacket::read);

    private static LidarDebugPacket read(FriendlyByteBuf buf) {
        long lidar = buf.readLong();
        Vec3 origin = buf.readVec3();
        byte[] kinds = buf.readByteArray();
        float[] ends = new float[kinds.length * 3];
        for (int i = 0; i < ends.length; i++) ends[i] = buf.readFloat();
        return new LidarDebugPacket(lidar, origin, ends, kinds);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeLong(lidar);
        buf.writeVec3(origin);
        buf.writeByteArray(kinds);
        for (float f : ends) buf.writeFloat(f);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> LidarDebugRenderer.show(lidar, origin, ends, kinds));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
