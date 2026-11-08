package dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import dev.propulsionteam.propulsionsimulated.content.thruster.SoundMufflerBehaviour;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.utility.DirectionalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RcsThrusterBlock extends DirectionalBlock implements IBE<RcsThrusterBlockEntity>, IWrenchable {
    public static final MapCodec<RcsThrusterBlock> CODEC = simpleCodec(RcsThrusterBlock::new);

    public RcsThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() { return CODEC; }

    public boolean isSingle() { return false; }

    public static Direction normal(BlockState state) {
        Direction facing = state.getValue(FACING);
        return ((RcsThrusterBlock) state.getBlock()).isSingle() ? facing.getOpposite() : facing;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = isSingle() ? DirectionalPlacement.nearestLookingDirection(context) : context.getClickedFace();
        if (context.getPlayer() != null ? context.getPlayer().isShiftKeyDown() : isSingle()) facing = facing.getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return RcsShapes.get(isSingle(), normal(state));
    }

    @Override
    public Class<RcsThrusterBlockEntity> getBlockEntityClass() { return RcsThrusterBlockEntity.class; }

    @Override
    public BlockEntityType<? extends RcsThrusterBlockEntity> getBlockEntityType() { return PropulsionBlockEntities.RCS_THRUSTER_BLOCK_ENTITY.get(); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == getBlockEntityType() ? new SmartBlockEntityTicker<>() : null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!level.isClientSide) withBlockEntityDo(level, pos, RcsThrusterBlockEntity::refreshPower);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighbor, boolean moved) {
        if (!level.isClientSide) withBlockEntityDo(level, pos, RcsThrusterBlockEntity::refreshPower);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (SoundMufflerBehaviour.handleInteraction(level.getBlockEntity(pos), player, hand)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hitResult) {
        if (SoundMufflerBehaviour.handleInteraction(level.getBlockEntity(pos), player, InteractionHand.MAIN_HAND)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        IBE.onRemove(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moved);
    }
}
