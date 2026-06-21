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

// Original single-block creative thruster value box behavior.
public class CreativeThrusterValueBox extends ValueBoxTransform.Sided {
    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(8, 8, 12.5f);
    }

    @Override
    public Vec3 getLocalOffset(final LevelAccessor level, final BlockPos pos, final BlockState state) {
        final Direction facing = state.getValue(CreativeThrusterBlock.FACING);
        return super.getLocalOffset(level, pos, state).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(3 / 16.0));
    }

    @Override
    public void rotate(final LevelAccessor level, final BlockPos pos, final BlockState state, final PoseStack ms) {
        super.rotate(level, pos, state, ms);
        final Direction facing = state.getValue(CreativeThrusterBlock.FACING);
        if (this.getSide().getAxis() == Axis.Y) {
            TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180);
        }
    }

    @Override
    protected boolean isSideActive(final BlockState state, final Direction side) {
        if (state.hasProperty(dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock.MULTIBLOCK)
                && state.getValue(dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock.MULTIBLOCK)) {
            return side == state.getValue(CreativeThrusterBlock.FACING);
        }
        final Direction thrusterFacing = state.getValue(CreativeThrusterBlock.FACING);
        final Direction placementFacing = state.getValue(CreativeThrusterBlock.PLACEMENT_FACING);
        if (side.getAxis() == thrusterFacing.getAxis()) {
            return false;
        }
        if (side == placementFacing) {
            return false;
        }
        return true;
    }

    @Override
    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
        if (level.getBlockEntity(pos) instanceof CreativeThrusterBlockEntity be
                && be.isMultiblock()
                && !be.isBaseLayerMember()) {
            return false;
        }
        return super.testHit(level, pos, state, localHit);
    }
}
