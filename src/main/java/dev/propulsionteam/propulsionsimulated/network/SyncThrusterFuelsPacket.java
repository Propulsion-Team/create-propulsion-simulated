package dev.propulsionteam.propulsionsimulated.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.thruster.FluidThrusterProperties;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterFuelManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

public class SyncThrusterFuelsPacket implements CustomPacketPayload {
    public static final Type<SyncThrusterFuelsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "sync_thruster_fuels"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncThrusterFuelsPacket> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> payload.encode(buf),
        buf -> SyncThrusterFuelsPacket.decode(buf)
    );

    private final Map<ResourceLocation, FluidThrusterProperties> fuelMap;
    private final Set<ResourceLocation> removedFuelIds;
    private final Map<ResourceLocation, Float> efficiencyOverrides;

    public static SyncThrusterFuelsPacket create(Map<Fluid, FluidThrusterProperties> mapToSync,
                                                 Set<ResourceLocation> removedFuelIds,
                                                 Map<ResourceLocation, Float> efficiencyOverrides) {
        Map<ResourceLocation, FluidThrusterProperties> networkSafeMap = new HashMap<>();
        mapToSync.forEach((fluid, props) -> {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
            if (key != null) {
                networkSafeMap.put(key, props);
            }
        });
        return new SyncThrusterFuelsPacket(networkSafeMap, removedFuelIds, efficiencyOverrides);
    }

    private SyncThrusterFuelsPacket(Map<ResourceLocation, FluidThrusterProperties> fuelMap,
                                    Set<ResourceLocation> removedFuelIds,
                                    Map<ResourceLocation, Float> efficiencyOverrides) {
        this.fuelMap = fuelMap;
        this.removedFuelIds = removedFuelIds;
        this.efficiencyOverrides = efficiencyOverrides;
    }

    public static SyncThrusterFuelsPacket decode(FriendlyByteBuf buf) {
        Map<ResourceLocation, FluidThrusterProperties> map = buf.readMap(FriendlyByteBuf::readResourceLocation, FluidThrusterProperties::decode);
        Set<ResourceLocation> removedFuelIds = buf.readCollection(java.util.HashSet::new, FriendlyByteBuf::readResourceLocation);
        Map<ResourceLocation, Float> efficiencyOverrides = buf.readMap(
            FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readFloat);
        return new SyncThrusterFuelsPacket(map, removedFuelIds, efficiencyOverrides);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(this.fuelMap, FriendlyByteBuf::writeResourceLocation, (b, props) -> props.encode(b));
        buf.writeCollection(this.removedFuelIds, FriendlyByteBuf::writeResourceLocation);
        buf.writeMap(this.efficiencyOverrides, FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeFloat);
    }

    public void handle() {
        ThrusterFuelManager.updateClient(this.fuelMap, this.removedFuelIds, this.efficiencyOverrides);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
