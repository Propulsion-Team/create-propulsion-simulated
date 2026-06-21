package dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock;

// Multiblock-only valuebox attached per base-layer block on back face.
public class CreativeThrusterMultiblockBackValueBox extends ValueBoxTransform.Sided {
    @Override
    protected Vec3 getSouthLocation() {
        // Full-block back-face center, slightly inset before the outward nudge.
        return VecHelper.voxelSpace(8, 8, 15.5f);
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof CreativeThrusterBlockEntity be) || !be.isMultiblock()) {
            return VecHelper.voxelSpace(8, 8, 8);
        }
        Direction facing = state.getValue(CreativeThrusterBlock.FACING);
        return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(3 / 16.0));
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        super.rotate(level, pos, state, ms);
        Direction facing = state.getValue(CreativeThrusterBlock.FACING);
        if (this.getSide().getAxis() == Axis.Y) {
            TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction side) {
        if (!state.hasProperty(ThrusterBlock.MULTIBLOCK) || !state.getValue(ThrusterBlock.MULTIBLOCK)) {
            return false;
        }
        return side == state.getValue(CreativeThrusterBlock.FACING);
    }

    @Override
    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
        if (!(level.getBlockEntity(pos) instanceof CreativeThrusterBlockEntity be)) {
            return false;
        }
        if (!be.isMultiblock() || !be.isBaseLayerMember()) {
            return false;
        }
        return super.testHit(level, pos, state, localHit);
    }
}
