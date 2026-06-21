package dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionShapes;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock;
import com.mojang.serialization.MapCodec;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CreativeThrusterBlock extends AbstractThrusterBlock implements IWrenchable {
    @Override
    public Class<AbstractThrusterBlockEntity> getBlockEntityClass() {
        return AbstractThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AbstractThrusterBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get();
    }
    public static final MapCodec<CreativeThrusterBlock> CODEC = simpleCodec(CreativeThrusterBlock::new);
    public static final DirectionProperty PLACEMENT_FACING = DirectionProperty.create("placement_facing", Direction.values());

    public CreativeThrusterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(PLACEMENT_FACING, Direction.DOWN)
                .setValue(ThrusterBlock.MULTIBLOCK, false));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLACEMENT_FACING);
        builder.add(ThrusterBlock.MULTIBLOCK);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new CreativeThrusterBlockEntity(PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;

        Direction placedAgainst = context.getClickedFace().getOpposite();
        return state.setValue(PLACEMENT_FACING, placedAgainst);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return PropulsionShapes.CREATIVE_THRUSTER.get(Direction.NORTH);
        }
        if (pState.hasProperty(ThrusterBlock.MULTIBLOCK) && pState.getValue(ThrusterBlock.MULTIBLOCK)) {
            return Shapes.block();
        }
        Direction direction = pState.getValue(FACING);
        if (direction == Direction.UP || direction == Direction.DOWN) direction = direction.getOpposite();
        return PropulsionShapes.CREATIVE_THRUSTER.get(direction);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CreativeThrusterBlockEntity thruster) {
                CreativeThrusterBlockEntity controller = thruster.isController() ? thruster : thruster.getControllerBE();
                if (controller != null) {
                    controller.disassembleMulti();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof CreativeThrusterBlockEntity creativeBe) {
                CreativeThrusterBlockEntity target = creativeBe;
                if (creativeBe.isMultiblock() && !creativeBe.isController()) {
                    CreativeThrusterBlockEntity ctrl = creativeBe.getControllerBE();
                    if (ctrl != null) {
                        target = ctrl;
                    }
                }
                target.cyclePlumeType();
                IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}
