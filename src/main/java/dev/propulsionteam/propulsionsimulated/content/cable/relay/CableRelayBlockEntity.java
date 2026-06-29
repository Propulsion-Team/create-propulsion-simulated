package dev.propulsionteam.propulsionsimulated.content.cable.relay;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
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
    // How often (in ticks) the controller re-scans for connected energy machines.
    // Network topology cache is invalidated immediately on block changes via markNetworkDirty().
    private static final int ENDPOINT_REBUILD_INTERVAL = 20;

    private int redstoneSignalStrength;
    private int relayId = -1;

    // Network topology cache - rebuilt via markNetworkDirty() on block changes.
    private boolean networkDirty = true;
    private Set<BlockPos> cachedNetwork = null;
    private boolean isController = false;

    // Endpoint location cache - rebuilt every ENDPOINT_REBUILD_INTERVAL ticks by the controller.
    // Stores (machinePos, machineFace) so capability is fetched fresh each tick (avoids stale refs).
    // I hate java
    private List<EndpointLocation> cachedEndpointLocations = null;
    private int endpointRebuildTimer = ENDPOINT_REBUILD_INTERVAL;

    public CableRelayBlockEntity(BlockPos pos, BlockState blockState) {
        super(PropulsionBlockEntities.CABLE_RELAY_BLOCK_ENTITY.get(), pos, blockState);
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
        networkDirty = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        if (networkDirty) {
            rebuildNetworkCache();
            networkDirty = false;
        }

        // Non-controllers exit here - no BFS, no capability lookups.
        if (!isController) return;

        if (cachedEndpointLocations == null || ++endpointRebuildTimer >= ENDPOINT_REBUILD_INTERVAL) {
            rebuildEndpointLocations();
            endpointRebuildTimer = 0;
        }

        tickRelayNetwork();
    }

    private void rebuildNetworkCache() {
        cachedNetwork = collectNetwork(worldPosition);
        BlockPos controller = cachedNetwork.stream()
            .min(Comparator.comparingInt((BlockPos p) -> p.getY())
                .thenComparingInt(p -> p.getX())
                .thenComparingInt(p -> p.getZ()))
            .orElse(worldPosition);
        isController = worldPosition.equals(controller);
        // Force endpoint rebuild on the next controller tick.
        cachedEndpointLocations = null;
        endpointRebuildTimer = ENDPOINT_REBUILD_INTERVAL;
    }

    private void rebuildEndpointLocations() {
        if (cachedNetwork == null) return;
        List<EndpointLocation> locations = new ArrayList<>();
        for (BlockPos nodePos : cachedNetwork) {
            BlockState state = level.getBlockState(nodePos);
            boolean nodeIsCable = state.getBlock() instanceof FeCableBlock;
            for (Direction direction : Direction.values()) {
                if (nodeIsCable && (!FeCableBlock.isSideEnabled(state, direction)
                        || !FeCableBlock.isSideConnected(state, direction))) {
                    continue;
                }
                BlockPos neighborPos = nodePos.relative(direction);
                if (isTransitNode(neighborPos)) continue;
                IEnergyStorage cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
                if (cap != null) {
                    locations.add(new EndpointLocation(neighborPos, direction.getOpposite()));
                }
            }
        }
        cachedEndpointLocations = locations;
    }

    private void tickRelayNetwork() {
        if (cachedNetwork == null || cachedEndpointLocations == null) return;

        List<Endpoint> sources = new ArrayList<>();
        List<Endpoint> sinks = new ArrayList<>();

        for (EndpointLocation loc : cachedEndpointLocations) {
            IEnergyStorage cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, loc.pos(), loc.face());
            if (cap == null) continue;
            if (cap.canExtract()) sources.add(new Endpoint(loc.pos(), loc.face(), cap));
            if (cap.canReceive()) sinks.add(new Endpoint(loc.pos(), loc.face(), cap));
        }

        if (sources.isEmpty() || sinks.isEmpty()) return;
        sources.sort(Endpoint::compare);
        sinks.sort(Endpoint::compare);

        int budget = Math.max(0, PropulsionConfig.CABLE_ENERGY_TRANSFER.get()) * cachedNetwork.size();
        if (budget <= 0) return;

        boolean madeProgress;
        do {
            madeProgress = false;

            int activeSinkCount = 0;
            for (Endpoint sink : sinks) {
                if (sink.storage.receiveEnergy(Math.max(1, budget / Math.max(1, sinks.size())), true) > 0)
                    activeSinkCount++;
            }
            if (activeSinkCount <= 0) break;

            int baseShare = Math.max(1, budget / activeSinkCount);
            int remainder = budget % activeSinkCount;

            for (Endpoint sink : sinks) {
                if (budget <= 0) break;
                int request = baseShare;
                if (remainder > 0) {
                    request++;
                    remainder--;
                }
                request = Math.min(request, budget);
                int canAccept = sink.storage.receiveEnergy(request, true);
                if (canAccept <= 0) continue;
                int extracted = extractFromSources(sources, canAccept);
                if (extracted <= 0) continue;
                int accepted = sink.storage.receiveEnergy(extracted, false);
                if (accepted <= 0) continue;
                budget -= accepted;
                madeProgress = true;
            }
        } while (budget > 0 && madeProgress);
    }

    private int extractFromSources(List<Endpoint> sources, int needed) {
        int remaining = needed;
        int totalExtracted = 0;
        for (Endpoint source : sources) {
            if (remaining <= 0) break;
            int available = source.storage.extractEnergy(remaining, true);
            if (available <= 0) continue;
            int extracted = source.storage.extractEnergy(available, false);
            if (extracted <= 0) continue;
            totalExtracted += extracted;
            remaining -= extracted;
        }
        return totalExtracted;
    }

    private Set<BlockPos> collectNetwork(BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current)) continue;

            BlockState state = level.getBlockState(current);
            boolean currentIsCable = state.getBlock() instanceof FeCableBlock;
            boolean currentIsRelay = level.getBlockEntity(current) instanceof CableRelayBlockEntity;
            if (!currentIsCable && !currentIsRelay) continue;

            for (Direction direction : Direction.values()) {
                if (currentIsCable && (!FeCableBlock.isSideEnabled(state, direction)
                        || !FeCableBlock.isSideConnected(state, direction))) {
                    continue;
                }

                BlockPos neighbor = current.relative(direction);
                var be = level.getBlockEntity(neighbor);
                boolean neighborIsCable = be instanceof FeCableBlockEntity;
                boolean neighborIsRelay = be instanceof CableRelayBlockEntity;
                if (!neighborIsCable && !neighborIsRelay) continue;

                if (neighborIsCable) {
                    BlockState neighborState = level.getBlockState(neighbor);
                    if (!FeCableBlock.isSideEnabled(neighborState, direction.getOpposite())
                            || !FeCableBlock.isSideConnected(neighborState, direction.getOpposite())) {
                        continue;
                    }
                }

                if (!visited.contains(neighbor)) queue.add(neighbor);
            }
        }
        return visited;
    }

    private boolean isTransitNode(BlockPos pos) {
        var be = level != null ? level.getBlockEntity(pos) : null;
        return be instanceof FeCableBlockEntity || be instanceof CableHubBlockEntity || be instanceof CableRelayBlockEntity;
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

    private record EndpointLocation(BlockPos pos, Direction face) {}

    private record Endpoint(BlockPos pos, Direction face, IEnergyStorage storage) {
        private static int compare(Endpoint a, Endpoint b) {
            int byY = Integer.compare(a.pos.getY(), b.pos.getY());
            if (byY != 0) return byY;
            int byX = Integer.compare(a.pos.getX(), b.pos.getX());
            if (byX != 0) return byX;
            int byZ = Integer.compare(a.pos.getZ(), b.pos.getZ());
            if (byZ != 0) return byZ;
            return Integer.compare(a.face.ordinal(), b.face.ordinal());
        }
    }
}
