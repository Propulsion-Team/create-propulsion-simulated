package dev.propulsionteam.propulsionsimulated.content.cable.fe;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.propulsionteam.propulsionsimulated.content.cable.relay.CableRelayBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.cable.CableNetworkManager;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FeCableBlock extends Block implements IBE<FeCableBlockEntity>, IWrenchable {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH_DISABLED = BooleanProperty.create("north_disabled");
    public static final BooleanProperty EAST_DISABLED = BooleanProperty.create("east_disabled");
    public static final BooleanProperty SOUTH_DISABLED = BooleanProperty.create("south_disabled");
    public static final BooleanProperty WEST_DISABLED = BooleanProperty.create("west_disabled");
    public static final BooleanProperty UP_DISABLED = BooleanProperty.create("up_disabled");
    public static final BooleanProperty DOWN_DISABLED = BooleanProperty.create("down_disabled");
    private final VoxelShape[] boundingShapes;

    public FeCableBlock(Properties properties) {
        super(properties);
        this.boundingShapes = createShapes(
            Block.box(6, 6, 6, 10, 10, 10),
            Block.box(6, 6, 0, 10, 10, 6),
            Block.box(10, 6, 6, 16, 10, 10),
            Block.box(6, 6, 10, 10, 10, 16),
            Block.box(0, 6, 6, 6, 10, 10),
            Block.box(6, 10, 6, 10, 16, 10),
            Block.box(6, 0, 6, 10, 6, 10)
        );
        registerDefaultState(defaultBlockState()
            .setValue(NORTH, false)
            .setValue(EAST, false)
            .setValue(SOUTH, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
            .setValue(NORTH_DISABLED, false)
            .setValue(EAST_DISABLED, false)
            .setValue(SOUTH_DISABLED, false)
            .setValue(WEST_DISABLED, false)
            .setValue(UP_DISABLED, false)
            .setValue(DOWN_DISABLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, NORTH_DISABLED, EAST_DISABLED, SOUTH_DISABLED, WEST_DISABLED, UP_DISABLED, DOWN_DISABLED);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return refreshConnections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean disabled = state.getValue(disabledPropertyFor(direction));
        CableNetworkManager.invalidateAround(level, pos);
        return state.setValue(propertyFor(direction), !disabled && canConnect(level, neighborPos, direction.getOpposite()));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            CableNetworkManager.invalidateAround(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static BlockState refreshConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction direction : Direction.values()) {
            boolean disabled = state.getValue(disabledPropertyFor(direction));
            state = state.setValue(propertyFor(direction), !disabled && canConnect(level, pos.relative(direction), direction.getOpposite()));
        }
        return state;
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private static BooleanProperty disabledPropertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH_DISABLED;
            case EAST -> EAST_DISABLED;
            case SOUTH -> SOUTH_DISABLED;
            case WEST -> WEST_DISABLED;
            case UP -> UP_DISABLED;
            case DOWN -> DOWN_DISABLED;
        };
    }

    public static boolean isSideEnabled(BlockState state, Direction direction) {
        return !state.getValue(disabledPropertyFor(direction));
    }

    public static boolean isSideConnected(BlockState state, Direction direction) {
        return state.getValue(propertyFor(direction));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return boundingShapes[packStates(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return boundingShapes[packStates(state)];
    }

    private static VoxelShape[] createShapes(VoxelShape inner, VoxelShape north, VoxelShape east, VoxelShape south, VoxelShape west, VoxelShape up, VoxelShape down) {
        VoxelShape[] shapes = new VoxelShape[64];
        for (int i = 0; i <= 63; i++) {
            VoxelShape shape = inner;
            if ((i & 1) != 0) shape = Shapes.joinUnoptimized(shape, north, BooleanOp.OR);
            if ((i & 2) != 0) shape = Shapes.joinUnoptimized(shape, east, BooleanOp.OR);
            if ((i & 4) != 0) shape = Shapes.joinUnoptimized(shape, south, BooleanOp.OR);
            if ((i & 8) != 0) shape = Shapes.joinUnoptimized(shape, west, BooleanOp.OR);
            if ((i & 16) != 0) shape = Shapes.joinUnoptimized(shape, up, BooleanOp.OR);
            if ((i & 32) != 0) shape = Shapes.joinUnoptimized(shape, down, BooleanOp.OR);
            shapes[i] = shape.optimize();
        }
        return shapes;
    }

    private static int packStates(BlockState state) {
        int i = 0;
        if (state.getValue(NORTH)) i |= 1;
        if (state.getValue(EAST)) i |= 2;
        if (state.getValue(SOUTH)) i |= 4;
        if (state.getValue(WEST)) i |= 8;
        if (state.getValue(UP)) i |= 16;
        if (state.getValue(DOWN)) i |= 32;
        return i;
    }

    public static boolean canConnect(LevelAccessor level, BlockPos neighborPos, Direction sideOnNeighbor) {
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (be instanceof FeCableBlockEntity) {
            BlockState neighborState = ((FeCableBlockEntity) be).getBlockState();
            return !neighborState.getValue(disabledPropertyFor(sideOnNeighbor));
        }
        if (be instanceof dev.propulsionteam.propulsionsimulated.content.cable.hub.CableHubBlockEntity) {
            return true;
        }
        if (be instanceof CableRelayBlockEntity) {
            return true;
        }
        if (!(level instanceof Level realLevel)) {
            return false;
        }
        var storage = realLevel.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, neighborPos, sideOnNeighbor);
        return storage != null;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Direction side = context.getClickedFace();
        BooleanProperty disableProperty = disabledPropertyFor(side);
        BlockState toggled = state.setValue(disableProperty, !state.getValue(disableProperty));
        BlockState updated = refreshConnections(level, context.getClickedPos(), toggled);
        level.setBlockAndUpdate(context.getClickedPos(), updated);
        IWrenchable.playRotateSound(level, context.getClickedPos());
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<FeCableBlockEntity> getBlockEntityClass() {
        return FeCableBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FeCableBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.FE_CABLE_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.FE_CABLE_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}
