package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Both angle value boxes on the model's top face (+Y): inset from the sides, centered on the face depth axis,
 * left and right box centers with a 1-voxel gap between them. Position rotates with blockstate.
 */
public class AdvancedTiltAdapterAngleValueBox extends ValueBoxTransform {
    /** Extra spread toward the left/right edges of the face (1/16-block pixels). */
    private static final float SIDE_OFFSET_PIXELS = 2.5f;
    private static final float DEPTH_CENTER = 8f;
    private static final float INSET = 1f;
    private static final float LEFT_X = INSET + 4.5f - SIDE_OFFSET_PIXELS;
    private static final float RIGHT_X = 16f - INSET - 4.5f + SIDE_OFFSET_PIXELS;

    private final boolean positionedOnLeft;

    public AdvancedTiltAdapterAngleValueBox(boolean positionedOnLeft) {
        this.positionedOnLeft = positionedOnLeft;
    }

    private Direction getFace(BlockState state) {
        return AbstractTiltAdapterBlock.getValueBoxFace(state);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        float x = positionedOnLeft ? LEFT_X : RIGHT_X;
        Vec3 onModelTop = VecHelper.voxelSpace(x, AbstractTiltAdapterBlock.VALUE_BOX_MODEL_TOP, DEPTH_CENTER);
        return AbstractTiltAdapterBlock.applyBlockstateModelRotation(onModelTop, state);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        Direction face = getFace(state);
        float yRot = AngleHelper.horizontalAngle(face) + 180;
        // Model Y spin only affects labels on sky/bottom faces (horizontal shaft placements).
        if (face == Direction.UP || face == Direction.DOWN) {
            yRot += AbstractTiltAdapterBlock.getBlockstateModelYRotation(state);
        }
        float xRot = face == Direction.UP ? 90 : face == Direction.DOWN ? 270 : 0;
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
        ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));
    }

    @Override
    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
        if (dominantFace(localHit) != getFace(state)) {
            return false;
        }
        return localHit.distanceTo(getLocalOffset(level, pos, state)) < getScale() / 3.5f;
    }

    private static Direction dominantFace(Vec3 localHit) {
        Direction best = Direction.UP;
        double bestDist = Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            double dist = distanceToFacePlane(localHit, face);
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
}
