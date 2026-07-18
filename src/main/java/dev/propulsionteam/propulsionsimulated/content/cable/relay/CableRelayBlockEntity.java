package dev.propulsionteam.propulsionsimulated.content.cable.relay;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.cable.CableNetworkManager;
import dev.propulsionteam.propulsionsimulated.content.cable.fe.FeCableBlock;
import dev.propulsionteam.propulsionsimulated.content.cable.fe.FeCableBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.cable.hub.CableHubBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CableRelayBlockEntity extends SmartBlockEntity {
    private int redstoneSignalStrength;
    private int relayId = -1;

    public CableRelayBlockEntity(BlockPos pos, BlockState blockState) {
        super(PropulsionBlockEntities.CABLE_RELAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null) {
            CableNetworkManager.invalidateAround(level, worldPosition);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public int getRedstoneSignalStrength() {
        return redstoneSignalStrength;
    }

    public void setRedstoneSignalStrength(int signalStrength) {
        redstoneSignalStrength = Math.max(0, Math.min(15, signalStrength));
    }

    public int getRelayId() {
        return relayId;
    }

    public void setRelayId(int relayId) {
        int clampedRelayId = Math.max(0, relayId);
        if (this.relayId == clampedRelayId) return;
        this.relayId = clampedRelayId;
        setChanged();
    }

    // Called by CableRelayBlock.updateCluster() when the relay cluster changes.
    public void markNetworkDirty() {
        if (level != null) {
            CableNetworkManager.invalidateAround(level, worldPosition);
        }
    }

    @Override
    public void tick() {
        super.tick();
        CableNetworkManager.tick(level, worldPosition);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putInt("RelayId", relayId);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        relayId = tag.contains("RelayId") ? Math.max(0, tag.getInt("RelayId")) : -1;
        super.read(tag, registries, clientPacket);
    }

}
