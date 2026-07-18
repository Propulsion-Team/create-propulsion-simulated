package dev.propulsionteam.propulsionsimulated.content.cable.relay;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.content.cable.CableNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CableRelayBlock extends Block implements IBE<CableRelayBlockEntity>, IWrenchable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static boolean clusterUpdateInProgress = false;

    public CableRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide || clusterUpdateInProgress) return;
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide) return;
        CableNetworkManager.invalidateAround(level, pos);
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            CableNetworkManager.invalidateAround(level, pos);
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                if (level.getBlockState(neighbor).getBlock() instanceof CableRelayBlock) {
                    level.scheduleTick(neighbor, this, 1);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateCluster(level, pos);
    }

    private static void updateCluster(Level level, BlockPos start) {
        if (clusterUpdateInProgress) return;
        clusterUpdateInProgress = true;
        try {
        Set<BlockPos> cluster = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!cluster.add(current)) continue;
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (level.getBlockState(neighbor).getBlock() instanceof CableRelayBlock && !cluster.contains(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        List<RelayNode> relays = collectRelayNodes(level, cluster);
        normalizeRelayIds(relays);

        for (RelayNode relay : relays) {
            relay.be().markNetworkDirty();
        }

        // Clear cluster output first so source detection cannot be fed by this
        // same ordered relay chain.
        for (RelayNode relay : relays) {
            relay.be().setRedstoneSignalStrength(0);
            BlockState relayState = level.getBlockState(relay.pos());
            if (relayState.getBlock() instanceof CableRelayBlock && relayState.getValue(POWERED)) {
                level.setBlock(relay.pos(), relayState.setValue(POWERED, false), Block.UPDATE_ALL);
            }
        }
        for (RelayNode relay : relays) {
            updateRelayNeighbors(level, relay.pos());
        }

        int clusterSignal = 0;
        for (RelayNode relay : relays) {
            clusterSignal = Math.max(clusterSignal, getExternalSignal(level, relay.pos()));
            if (clusterSignal >= 15) {
                break;
            }
        }

        boolean powered = clusterSignal > 0;
        for (RelayNode relay : relays) {
            relay.be().setRedstoneSignalStrength(clusterSignal);
            BlockState relayState = level.getBlockState(relay.pos());
            if (relayState.getBlock() instanceof CableRelayBlock && relayState.getValue(POWERED) != powered) {
                level.setBlock(relay.pos(), relayState.setValue(POWERED, powered), Block.UPDATE_ALL);
            } else {
                updateRelayNeighbors(level, relay.pos());
            }
        }
        } finally {
            clusterUpdateInProgress = false;
        }
    }

    private static List<RelayNode> collectRelayNodes(Level level, Set<BlockPos> cluster) {
        List<RelayNode> relays = new ArrayList<>();
        for (BlockPos relayPos : cluster) {
            BlockEntity be = level.getBlockEntity(relayPos);
            if (be instanceof CableRelayBlockEntity relayBe) {
                relays.add(new RelayNode(relayPos, relayBe));
            }
        }
        return relays;
    }

    private static void normalizeRelayIds(List<RelayNode> relays) {
        relays.sort(Comparator
            .comparingInt((RelayNode relay) -> relay.be().getRelayId() < 0 ? Integer.MAX_VALUE : relay.be().getRelayId())
            .thenComparing(RelayNode::pos, CableRelayBlock::compareBlockPos));

        for (int i = 0; i < relays.size(); i++) {
            relays.get(i).be().setRelayId(i);
        }
    }

    private static int getExternalSignal(Level level, BlockPos relayPos) {
        int maxSignal = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = relayPos.relative(dir);
            if (level.getBlockState(neighborPos).getBlock() instanceof CableRelayBlock) continue;

            Direction towardRelay = dir.getOpposite();
            int weak = level.getSignal(neighborPos, towardRelay);
            int strong = level.getDirectSignal(neighborPos, towardRelay);
            maxSignal = Math.max(maxSignal, Math.max(weak, strong));
            if (maxSignal >= 15) {
                return maxSignal;
            }
        }
        return maxSignal;
    }

    private static void updateRelayNeighbors(Level level, BlockPos relayPos) {
        BlockState relayState = level.getBlockState(relayPos);
        level.updateNeighborsAt(relayPos, relayState.getBlock());
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = relayPos.relative(dir);
            level.updateNeighborsAt(neighborPos, level.getBlockState(neighborPos).getBlock());
        }
    }

    private static int compareBlockPos(BlockPos a, BlockPos b) {
        int byY = Integer.compare(a.getY(), b.getY());
        if (byY != 0) return byY;
        int byX = Integer.compare(a.getX(), b.getX());
        if (byX != 0) return byX;
        return Integer.compare(a.getZ(), b.getZ());
    }

    private record RelayNode(BlockPos pos, CableRelayBlockEntity be) {}

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CableRelayBlockEntity relayBe) {
            return relayBe.getRedstoneSignalStrength();
        }
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CableRelayBlockEntity relayBe) {
            return relayBe.getRedstoneSignalStrength();
        }
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public Class<CableRelayBlockEntity> getBlockEntityClass() {
        return CableRelayBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CableRelayBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.CABLE_RELAY_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableRelayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.CABLE_RELAY_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}
