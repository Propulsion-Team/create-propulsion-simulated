package dev.propulsionteam.propulsionsimulated.content.thruster.thruster;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterShapes;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ThrusterBlock extends AbstractThrusterBlock {
    public static final MapCodec<ThrusterBlock> CODEC = simpleCodec(ThrusterBlock::new);
    public static final BooleanProperty MULTIBLOCK = BooleanProperty.create("multi");

    public ThrusterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(MULTIBLOCK, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MULTIBLOCK);
    }

    @Override
    protected VoxelShape getShape(final BlockState state,
                                  final BlockGetter level,
                                  final BlockPos pos,
                                  final CollisionContext context) {
        if (state.hasProperty(MULTIBLOCK) && state.getValue(MULTIBLOCK)) {
            return Shapes.block();
        }
        final Direction direction = state.getValue(FACING);
        return ThrusterShapes.THRUSTER.get(direction);
    }

    @Override
    public Class<AbstractThrusterBlockEntity> getBlockEntityClass() {
        return AbstractThrusterBlockEntity.class;
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ThrusterBlockEntity(PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public BlockEntityType<? extends AbstractThrusterBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ThrusterBlockEntity thruster) {
                ThrusterBlockEntity controller = thruster.isController() ? thruster : thruster.getControllerBE();
                if (controller != null) {
                    controller.disassembleMulti();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}
