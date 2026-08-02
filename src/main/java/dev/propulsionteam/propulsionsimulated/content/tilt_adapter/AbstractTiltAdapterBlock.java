package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionShapes;
import dev.propulsionteam.propulsionsimulated.utility.DirectionalPlacement;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractTiltAdapterBlock<T extends TiltAdapterBlockEntity> extends AbstractEncasedShaftBlock implements IBE<T> {
    public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");
    public static final BooleanProperty ALIGNED_X = BooleanProperty.create("aligned_x");
    public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE = DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;

    private final Class<T> blockEntityClass;
    private final Supplier<BlockEntityType<T>> blockEntityType;
    private final BlockEntityFactory<T> blockEntityFactory;

    @FunctionalInterface
    public interface BlockEntityFactory<T extends TiltAdapterBlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    protected AbstractTiltAdapterBlock(Properties properties, Class<T> blockEntityClass,
        Supplier<BlockEntityType<T>> blockEntityType, BlockEntityFactory<T> blockEntityFactory) {
        super(properties);
        this.blockEntityClass = blockEntityClass;
        this.blockEntityType = blockEntityType;
        this.blockEntityFactory = blockEntityFactory;
        registerDefaultState(defaultBlockState()
            .setValue(AXIS, Direction.Axis.Y)
            .setValue(DirectionalKineticBlock.FACING, Direction.UP)
            .setValue(AXIS_ALONG_FIRST_COORDINATE, false)
            .setValue(POSITIVE, true)
            .setValue(ALIGNED_X, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction baseDirection = DirectionalPlacement.nearestLookingDirection(context);
        Player player = context.getPlayer();
        boolean manualPlacement = player != null && player.isShiftKeyDown();
        Direction placeDirection;

        if (!manualPlacement) {
            Direction inputSide = getPreferredShaftInput(context);
            placeDirection = inputSide == null ? baseDirection.getOpposite() : inputSide.getOpposite();
        } else {
            placeDirection = baseDirection;
        }

        Direction.Axis axis = placeDirection.getAxis();
        boolean alignedX = axis == Direction.Axis.Y && context.getHorizontalDirection().getAxis() == Direction.Axis.X;
        return fromFacingAndAlignment(defaultBlockState(), placeDirection, alignedX);
    }

    @Nullable
    private static Direction getPreferredShaftInput(BlockPlaceContext context) {
        BlockPos placementPos = context.getClickedPos();
        Direction clickedInputSide = context.getClickedFace().getOpposite();

        if (hasShaftFacingPlacement(context, placementPos, clickedInputSide)) {
            return clickedInputSide;
        }

        Direction preferred = null;

        for (Direction side : Direction.values()) {
            if (!hasShaftFacingPlacement(context, placementPos, side)) {
                continue;
            }

            if (preferred != null && preferred.getAxis() != side.getAxis()) {
                return null;
            }
            preferred = side;
        }
        return preferred;
    }

    private static boolean hasShaftFacingPlacement(BlockPlaceContext context, BlockPos placementPos, Direction side) {
        BlockPos neighbourPos = placementPos.relative(side);
        BlockState neighbourState = context.getLevel().getBlockState(neighbourPos);
        return neighbourState.getBlock() instanceof IRotate rotate
            && rotate.hasShaftTowards(context.getLevel(), neighbourPos, neighbourState, side.getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (rot.ordinal() % 2 == 1) {
            state = state.cycle(AXIS_ALONG_FIRST_COORDINATE);
        }
        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
        return fromFacingAndAlignment(state, rot.rotate(facing), state.getValue(AXIS_ALONG_FIRST_COORDINATE));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return rotate(state, mirrorIn.getRotation(state.getValue(DirectionalKineticBlock.FACING)));
    }

    private static BlockState fromFacingAndAlignment(BlockState state, Direction facing, boolean axisAlongFirst) {
        Direction.Axis axis = facing.getAxis();
        boolean isPositive = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        boolean alignedX = false;
        if (axis == Direction.Axis.Y) {
            alignedX = axisAlongFirst;
        }

        return state
            .setValue(AXIS, axis)
            .setValue(DirectionalKineticBlock.FACING, facing)
            .setValue(AXIS_ALONG_FIRST_COORDINATE, axisAlongFirst)
            .setValue(POSITIVE, isPositive)
            .setValue(ALIGNED_X, alignedX);
    }

    public static boolean isAxisAlongFirst(BlockState state) {
        if (state.hasProperty(AXIS_ALONG_FIRST_COORDINATE)) {
            return state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        }
        return state.hasProperty(ALIGNED_X) && state.getValue(ALIGNED_X);
    }

    /** World direction of the left (positive differential) redstone input face. */
    public static boolean isShaftFace(BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    public static Direction getShaftFace(BlockState state, boolean positiveAlongAxis) {
        return Direction.fromAxisAndDirection(
            state.getValue(AXIS),
            positiveAlongAxis ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE
        );
    }

    /**
     * Applies the same rotations as {@code advanced_tilt_adapter.json} / {@code tilt_adapter.json} blockstates.
     */
    public static Vec3 applyBlockstateModelRotation(Vec3 vec, BlockState state) {
        Axis axis = state.getValue(AXIS);
        Direction facing = getDirection(state);
        boolean alongFirst = isAxisAlongFirst(state);

        if (axis == Axis.Z) {
            if (facing == Direction.SOUTH) {
                vec = VecHelper.rotateCentered(vec, 180, Axis.Y);
            }
        } else if (axis == Axis.X) {
            if (facing == Direction.WEST) {
                vec = VecHelper.rotateCentered(vec, 270, Axis.Y);
            } else if (facing == Direction.EAST) {
                vec = VecHelper.rotateCentered(vec, 90, Axis.Y);
            }
        } else if (axis == Axis.Y) {
            if (facing == Direction.UP) {
                vec = VecHelper.rotateCentered(vec, 270, Axis.X);
                if (alongFirst) {
                    vec = VecHelper.rotateCentered(vec, 90, Axis.Y);
                }
            } else if (facing == Direction.DOWN) {
                vec = VecHelper.rotateCentered(vec, 90, Axis.X);
                if (alongFirst) {
                    vec = VecHelper.rotateCentered(vec, 90, Axis.Y);
                }
            }
        }
        return vec;
    }

    /** Extra Y rotation from blockstate (degrees), for value-box label alignment. */
    public static float getBlockstateModelYRotation(BlockState state) {
        Axis axis = state.getValue(AXIS);
        Direction facing = getDirection(state);
        if (axis == Axis.Z && facing == Direction.SOUTH) {
            return 180;
        }
        if (axis == Axis.X) {
            if (facing == Direction.WEST) {
                return 270;
            }
            if (facing == Direction.EAST) {
                return 90;
            }
        }
        if (axis == Axis.Y && isAxisAlongFirst(state)) {
            return 90;
        }
        return 0;
    }

    /** Voxel Y of the model top surface (+Y) before blockstate rotation. */
    public static final float VALUE_BOX_MODEL_TOP = 16f;

    /** World face carrying the advanced value boxes (model +Y after blockstate rotation). */
    public static Direction getValueBoxFace(BlockState state) {
        Vec3 onModelTop = VecHelper.voxelSpace(8, VALUE_BOX_MODEL_TOP, 8);
        Vec3 rotated = applyBlockstateModelRotation(onModelTop, state);
        return faceFromLocalPosition(rotated);
    }

    private static Direction faceFromLocalPosition(Vec3 local) {
        Direction best = Direction.UP;
        double bestDist = Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            double dist = distanceToFacePlane(local, face);
            if (dist < bestDist) {
                bestDist = dist;
                best = face;
            }
        }
        return best;
    }

    private static double distanceToFacePlane(Vec3 local, Direction face) {
        return switch (face) {
            case WEST -> local.x;
            case EAST -> 1.0 - local.x;
            case DOWN -> local.y;
            case UP -> 1.0 - local.y;
            case NORTH -> local.z;
            case SOUTH -> 1.0 - local.z;
        };
    }

    public static Direction getRedstoneSide(BlockState state, boolean left) {
        Axis axis = state.getValue(AXIS);
        Direction facing = getDirection(state);
        boolean positiveDir = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        boolean alignedX = isAxisAlongFirst(state);

        Direction positiveSide = switch (axis) {
            case X -> Direction.SOUTH;
            case Z -> Direction.WEST;
            case Y -> alignedX ? Direction.NORTH : Direction.EAST;
        };
        Direction negativeSide = switch (axis) {
            case X -> Direction.NORTH;
            case Z -> Direction.EAST;
            case Y -> alignedX ? Direction.SOUTH : Direction.WEST;
        };

        if (!positiveDir) {
            Direction temp = positiveSide;
            positiveSide = negativeSide;
            negativeSide = temp;
        }

        return left ? positiveSide : negativeSide;
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        Direction direction = getDirection(pState);
        if (direction.getAxis() == Axis.Y) direction = direction.getOpposite();
        return PropulsionShapes.TILT_ADAPTER.get(direction);
    }

    public static Direction getDirection(BlockState state) {
        if (state.hasProperty(DirectionalKineticBlock.FACING)) {
            return state.getValue(DirectionalKineticBlock.FACING);
        }
        Direction.Axis axis = state.getValue(AXIS);
        boolean isPositive = state.getValue(POSITIVE);

        switch (axis) {
            case X:
                return isPositive ? Direction.EAST : Direction.WEST;
            case Y:
                return isPositive ? Direction.UP : Direction.DOWN;
            case Z:
                return isPositive ? Direction.SOUTH : Direction.NORTH;
            default:
                return Direction.UP;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AXIS, DirectionalKineticBlock.FACING, AXIS_ALONG_FIRST_COORDINATE, POSITIVE, ALIGNED_X);
    }

    @Override
    public Class<T> getBlockEntityClass() {
        return blockEntityClass;
    }

    @Override
    public BlockEntityType<T> getBlockEntityType() {
        return blockEntityType.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.create(pos, state);
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return false;
    }
}
