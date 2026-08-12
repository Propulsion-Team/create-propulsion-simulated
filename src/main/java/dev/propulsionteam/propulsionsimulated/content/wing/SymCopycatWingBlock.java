package dev.propulsionteam.propulsionsimulated.content.wing;

import java.util.List;
import java.util.function.Supplier;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionShapes;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import dev.ryanhcode.sable.api.block.BlockSubLevelCustomCenterOfMass;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.math.VoxelShaper;
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
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SymCopycatWingBlock extends CopycatBlock implements BlockSubLevelLiftProvider, BlockSubLevelCustomCenterOfMass, SpecialBlockItemRequirement {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private static final Vector3dc CENTER_OF_MASS = new Vector3d(0.5, 0.5, 0.5);
    private static final List<Supplier<? extends Block>> entires =
        List.of(
            PropulsionBlocks.SYMMETRIC_COPYCAT_WING,
            PropulsionBlocks.SYMMETRIC_COPYCAT_WING_8,
            PropulsionBlocks.SYMMETRIC_COPYCAT_WING_12,
            PropulsionBlocks.SYMMETRIC_WING_BLOCK
        );
    private static final int placementHelperId = PlacementHelpers.register(new SymWingPlacementHelper(entires));
    private final int width;

    private static final Map<Integer, VoxelShaper> wingShapers = Map.of(
        4, PropulsionShapes.SYMMETRIC_WING,
        8, PropulsionShapes.SYMMETRIC_WING_8,
        12, PropulsionShapes.SYMMETRIC_WING_12
    );

    public SymCopycatWingBlock(Properties properties, int width) {
        super(properties);
        this.width = width;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(AXIS, context.getNearestLookingDirection().getAxis());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
        if (player != null && !player.isShiftKeyDown() && player.mayBuild()) {
            IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
            if (placementHelper.matchesItem(stack) && stack.getItem() instanceof BlockItem blockItem) {
                var offset = placementHelper.getOffset(player, world, state, pos, ray);
                if (offset.isSuccessful()) {
                    if (world.isClientSide) return ItemInteractionResult.SUCCESS;
                    return offset.placeInWorld(world, blockItem, player, hand, ray);
                }
            }
        }
        return super.useItemOn(stack, state, world, pos, player, hand, ray);
    }

	public InteractionResult use(@Nonnull BlockState state, @Nonnull Level world, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult ray) {
        // Placement with an item is handled in useItemOn() to support Create's helper arrows.
        return InteractionResult.PASS;
    }

    public int getWidth() {
        return width;
    }

    @Override
    public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos, BlockState state) {
        BlockState toState = reader.getBlockState(toPos);
        if (!toState.is(this)) {
            return false;
        }

        return state.getValue(AXIS) == toState.getValue(AXIS);
    }

    @Override
    public boolean isIgnoredConnectivitySide(BlockAndTintGetter reader, BlockState state, Direction face, @Nullable BlockPos fromPos, @Nullable BlockPos toPos) {
        if (fromPos == null || toPos == null) return true;

        BlockState toState = reader.getBlockState(toPos);
        Direction.Axis axis = state.getValue(AXIS);

        if (!toState.is(this)) return axis != face.getAxis();

        Direction.Axis toAxis = toState.getValue(AXIS);
        BlockPos diff = toPos.subtract(fromPos);

        //Avoiding over-gap connections
        if (diff.getX() == 0 && diff.getZ() == 0 && diff.getY() != 0 && axis == Direction.Axis.Y && toAxis == Direction.Axis.Y) {
            return true;
        }
        if (diff.getY() == 0 && diff.getZ() == 0 && diff.getX() != 0 && axis == Direction.Axis.X && toAxis == Direction.Axis.X) {
            return true;
        }
        if (diff.getX() == 0 && diff.getY() == 0 && diff.getZ() != 0 && axis == Direction.Axis.Z && toAxis == Direction.Axis.Z) {
            return true;
        }

        return false;
    }

    @Override
    public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.SYMMETRIC_COPYCAT_WING_BLOCK_ENTITY.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (newState.getBlock() instanceof SymCopycatWingBlock) {
            if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
                level.removeBlockEntity(pos);
            }
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        BlockState material = getMaterial(level, pos);
        if (player != null && player.isShiftKeyDown()) {
            return new ItemStack(PropulsionBlocks.SYMMETRIC_COPYCAT_WING.get());
        }
        
        return material.getBlock().asItem().getDefaultInstance();
    }

    @Override
    public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        int dropCount = this.width / 4;
        if (dropCount < 1) {
            return Collections.emptyList();
        }
        return List.of(new ItemStack(PropulsionBlocks.SYMMETRIC_COPYCAT_WING.get(), dropCount));
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        return new ItemRequirement(
            ItemRequirement.ItemUseType.CONSUME,
            new ItemStack(PropulsionBlocks.SYMMETRIC_COPYCAT_WING.get(), Math.max(1, width / 4))
        );
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(AXIS));
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return wingShapers.get(this.width).get(Direction.Axis.Y);
        }
        return wingShapers.get(this.width).get(pState.getValue(AXIS));
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
