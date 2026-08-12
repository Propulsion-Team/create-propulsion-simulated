package dev.propulsionteam.propulsionsimulated.content.wing;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionShapes;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;

import dev.ryanhcode.sable.api.block.BlockSubLevelCustomCenterOfMass;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SymWingBlock extends RotatedPillarBlock implements BlockSubLevelLiftProvider, BlockSubLevelCustomCenterOfMass {
    private static final Vector3dc CENTER_OF_MASS = new Vector3d(0.5, 0.5, 0.5);
    private static final List<Supplier<? extends net.minecraft.world.level.block.Block>> entires =
        List.of(PropulsionBlocks.SYMMETRIC_WING_BLOCK);
    private static final int placementHelperId = PlacementHelpers.register(new SymWingPlacementHelper(entires));

    public SymWingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getNearestLookingDirection().getAxis());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        if (player == null || player.isShiftKeyDown() || !player.mayBuild()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (!placementHelper.matchesItem(stack) || !(stack.getItem() instanceof BlockItem blockItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        var offset = placementHelper.getOffset(player, world, state, pos, ray);
        if (!offset.isSuccessful()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (world.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        return offset.placeInWorld(world, blockItem, player, hand, ray);
    }

	public InteractionResult use(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
        // Placement with an item is handled in useItemOn() to support Create's helper arrows.
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return PropulsionShapes.SYMMETRIC_WING.get(Direction.Axis.Y);
        }
        return PropulsionShapes.SYMMETRIC_WING.get(pState.getValue(AXIS));
    }

    @Override
    public Direction sable$getNormal(BlockState state) {
        return Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS));
    }
    
    @Override
    public float sable$getLiftScalar() {
        return 0f;
    }

    @Override
    public float sable$getParallelDragScalar() {
        return 1.75f;
    }

    @Override
    public Vector3dc getCenterOfMass(BlockGetter level, BlockState state) {
        return CENTER_OF_MASS;
    }
}
