package dev.propulsionteam.propulsionsimulated.content.hydropump;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.cable.fe.FeCableBlock;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HydropumpBlock extends PumpBlock {
    private static final VoxelShape BODY = Block.box(2, 2, 2, 14, 14, 14);
    private static final VoxelShape SHAPE_X = Shapes.or(BODY, Block.box(0, 3, 3, 16, 13, 13));
    private static final VoxelShape SHAPE_Y = Shapes.or(BODY, Block.box(3, 0, 3, 13, 16, 13));
    private static final VoxelShape SHAPE_Z = Shapes.or(BODY, Block.box(3, 3, 0, 13, 13, 16));

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
        };
    }

    public HydropumpBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(connectionProperty(direction), false);
        }
        registerDefaultState(state);
    }

    public static BooleanProperty connectionProperty(Direction direction) {
        return switch (direction) {
            case DOWN -> BlockStateProperties.DOWN;
            case UP -> BlockStateProperties.UP;
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case WEST -> BlockStateProperties.WEST;
            case EAST -> BlockStateProperties.EAST;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        for (Direction direction : Direction.values()) {
            builder.add(connectionProperty(direction));
        }
    }

    public static BlockState updateConnections(BlockState state, Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            boolean connected = false;
            if (direction.getAxis() != state.getValue(FACING).getAxis()) {
                connected = FeCableBlock.canConnect(level, pos.relative(direction), direction.getOpposite());
            }
            state = state.setValue(connectionProperty(direction), connected);
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos,
                                boolean moving) {
        super.neighborChanged(state, level, pos, block, neighborPos, moving);
        if (!level.isClientSide) {
            BlockState updated = updateConnections(state, level, pos);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public boolean isSmallCog() {
        return false;
    }

    @Override
    public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.HYDROPUMP_BLOCK_ENTITY.get();
    }
}
