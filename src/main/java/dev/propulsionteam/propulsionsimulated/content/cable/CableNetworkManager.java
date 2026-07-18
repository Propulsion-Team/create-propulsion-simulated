package dev.propulsionteam.propulsionsimulated.content.cable;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.cable.fe.FeCableBlock;
import dev.propulsionteam.propulsionsimulated.content.cable.fe.FeCableBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.cable.relay.CableRelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Routes each connected cable/relay graph once per server tick. */
public final class CableNetworkManager {
    private static final int ENDPOINT_REBUILD_INTERVAL = 20;
    private static final Map<Level, LevelNetworks> LEVELS = new WeakHashMap<>();

    private CableNetworkManager() {}

    public static void tick(Level level, BlockPos nodePos) {
        if (level == null || level.isClientSide) {
            return;
        }
        LevelNetworks networks = networks(level);
        Network network = networks.byNode.get(nodePos);
        if (network == null || network.dirty
            || !level.hasChunkAt(network.controller)
            || !isNetworkNode(level, network.controller)) {
            network = rebuild(level, networks, nodePos);
        }
        if (network == null || !nodePos.equals(network.controller)) {
            return;
        }

        long gameTime = level.getGameTime();
        if (network.lastRouteTick == gameTime) {
            return;
        }
        network.lastRouteTick = gameTime;
        if (network.endpoints == null
            || gameTime - network.lastEndpointBuildTick >= ENDPOINT_REBUILD_INTERVAL) {
            network.endpoints = collectEndpoints(level, network.nodes);
            network.lastEndpointBuildTick = gameTime;
        }
        route(level, network);
    }

    public static void invalidateAround(LevelAccessor levelAccessor, BlockPos pos) {
        if (!(levelAccessor instanceof Level level) || level.isClientSide) {
            return;
        }
        LevelNetworks networks = networks(level);
        markDirty(networks.byNode.get(pos));
        for (Direction direction : Direction.values()) {
            markDirty(networks.byNode.get(pos.relative(direction)));
        }
    }

    private static void markDirty(Network network) {
        if (network != null) {
            network.dirty = true;
        }
    }

    private static LevelNetworks networks(Level level) {
        synchronized (LEVELS) {
            return LEVELS.computeIfAbsent(level, ignored -> new LevelNetworks());
        }
    }

    private static Network rebuild(Level level, LevelNetworks networks, BlockPos start) {
        if (!isNetworkNode(level, start)) {
            networks.byNode.remove(start);
            return null;
        }

        Set<BlockPos> nodes = collectNetwork(level, start);
        if (nodes.isEmpty()) {
            return null;
        }
        BlockPos controller = nodes.stream().min(CableNetworkManager::comparePos).orElse(start).immutable();
        Network network = new Network(controller, Set.copyOf(nodes));
        for (BlockPos node : nodes) {
            networks.byNode.put(node.immutable(), network);
        }
        return network;
    }

    private static Set<BlockPos> collectNetwork(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current) || !isNetworkNode(level, current)) {
                continue;
            }
            BlockState currentState = level.getBlockState(current);
            boolean currentCable = currentState.getBlock() instanceof FeCableBlock;
            for (Direction direction : Direction.values()) {
                if (currentCable && (!FeCableBlock.isSideEnabled(currentState, direction)
                    || !FeCableBlock.isSideConnected(currentState, direction))) {
                    continue;
                }
                BlockPos neighbor = current.relative(direction);
                if (!level.hasChunkAt(neighbor) || !isNetworkNode(level, neighbor)) {
                    continue;
                }
                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.getBlock() instanceof FeCableBlock
                    && (!FeCableBlock.isSideEnabled(neighborState, direction.getOpposite())
                    || !FeCableBlock.isSideConnected(neighborState, direction.getOpposite()))) {
                    continue;
                }
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor.immutable());
                }
            }
        }
        return visited;
    }

    private static List<EndpointLocation> collectEndpoints(Level level, Set<BlockPos> nodes) {
        List<EndpointLocation> endpoints = new ArrayList<>();
        for (BlockPos node : nodes) {
            BlockState state = level.getBlockState(node);
            boolean cable = state.getBlock() instanceof FeCableBlock;
            for (Direction direction : Direction.values()) {
                if (cable && (!FeCableBlock.isSideEnabled(state, direction)
                    || !FeCableBlock.isSideConnected(state, direction))) {
                    continue;
                }
                BlockPos neighbor = node.relative(direction);
                if (nodes.contains(neighbor)) {
                    continue;
                }
                Direction face = direction.getOpposite();
                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, face);
                if (storage != null) {
                    endpoints.add(new EndpointLocation(neighbor.immutable(), face));
                }
            }
        }
        endpoints.sort(EndpointLocation::compare);
        return endpoints;
    }

    private static void route(Level level, Network network) {
        if (network.endpoints == null || network.endpoints.isEmpty()) {
            return;
        }
        List<IEnergyStorage> sources = new ArrayList<>();
        List<IEnergyStorage> sinks = new ArrayList<>();
        for (EndpointLocation endpoint : network.endpoints) {
            IEnergyStorage storage = level.getCapability(
                Capabilities.EnergyStorage.BLOCK, endpoint.pos, endpoint.face);
            if (storage == null) {
                continue;
            }
            if (storage.canExtract()) sources.add(storage);
            if (storage.canReceive()) sinks.add(storage);
        }
        if (sources.isEmpty() || sinks.isEmpty()) {
            return;
        }

        int budget = Math.max(0, PropulsionConfig.CABLE_ENERGY_TRANSFER.get()) * network.nodes.size();
        boolean progressed;
        do {
            progressed = false;
            int activeSinks = 0;
            for (IEnergyStorage sink : sinks) {
                if (sink.receiveEnergy(Math.max(1, budget / Math.max(1, sinks.size())), true) > 0) {
                    activeSinks++;
                }
            }
            if (activeSinks == 0) break;

            int share = Math.max(1, budget / activeSinks);
            int remainder = budget % activeSinks;
            for (IEnergyStorage sink : sinks) {
                if (budget <= 0) break;
                int requested = Math.min(budget, share + (remainder-- > 0 ? 1 : 0));
                int acceptedSimulation = sink.receiveEnergy(requested, true);
                if (acceptedSimulation <= 0) continue;
                int extracted = extract(sources, acceptedSimulation);
                if (extracted <= 0) continue;
                int accepted = sink.receiveEnergy(extracted, false);
                if (accepted > 0) {
                    budget -= accepted;
                    progressed = true;
                }
            }
        } while (budget > 0 && progressed);
    }

    private static int extract(List<IEnergyStorage> sources, int requested) {
        int remaining = requested;
        for (IEnergyStorage source : sources) {
            if (remaining <= 0) break;
            int available = source.extractEnergy(remaining, true);
            if (available > 0) {
                remaining -= source.extractEnergy(available, false);
            }
        }
        return requested - remaining;
    }

    private static boolean isNetworkNode(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof FeCableBlockEntity || blockEntity instanceof CableRelayBlockEntity;
    }

    private static int comparePos(BlockPos first, BlockPos second) {
        int y = Integer.compare(first.getY(), second.getY());
        if (y != 0) return y;
        int x = Integer.compare(first.getX(), second.getX());
        if (x != 0) return x;
        return Integer.compare(first.getZ(), second.getZ());
    }

    private static final class LevelNetworks {
        private final Map<BlockPos, Network> byNode = new HashMap<>();
    }

    private static final class Network {
        private final BlockPos controller;
        private final Set<BlockPos> nodes;
        private boolean dirty;
        private List<EndpointLocation> endpoints;
        private long lastEndpointBuildTick = Long.MIN_VALUE;
        private long lastRouteTick = Long.MIN_VALUE;

        private Network(BlockPos controller, Set<BlockPos> nodes) {
            this.controller = controller;
            this.nodes = nodes;
        }
    }

    private record EndpointLocation(BlockPos pos, Direction face) {
        private static int compare(EndpointLocation first, EndpointLocation second) {
            int position = comparePos(first.pos, second.pos);
            return position != 0 ? position : Integer.compare(first.face.ordinal(), second.face.ordinal());
        }
    }
}
