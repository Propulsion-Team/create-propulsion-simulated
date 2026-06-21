package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CreativeVectorThrusterValueBox extends ValueBoxTransform.Dual {
    public CreativeVectorThrusterValueBox(boolean first) {
        super(first);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        // Centre of the north face of the input cube (x=8, y=8, z=0).
        Vec3 local = VecHelper.voxelSpace(8f, 8f, 0f);
        return rotatePointForFacing(local, state.getValue(CreativeVectorThrusterBlock.FACING));
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        Direction facing = state.getValue(CreativeVectorThrusterBlock.FACING);
        float yRot = AngleHelper.horizontalAngle(facing) + 180;
        float xRot = facing == Direction.UP ? 90 : facing == Direction.DOWN ? 270 : 0;
        ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
        ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));
    }

    @Override
    public float getScale() {
        return 0.4975f;
    }

    private Vec3 rotatePointForFacing(Vec3 vec, Direction blockFacing) {
        return switch (blockFacing) {
            case NORTH -> vec;
            case EAST  -> VecHelper.rotateCentered(vec, -90, Direction.Axis.Y);
            case SOUTH -> VecHelper.rotateCentered(vec, 180, Direction.Axis.Y);
            case WEST  -> VecHelper.rotateCentered(vec, 90, Direction.Axis.Y);
            case UP    -> VecHelper.rotateCentered(vec, 90, Direction.Axis.X);
            case DOWN  -> VecHelper.rotateCentered(vec, -90, Direction.Axis.X);
        };
    }
}
