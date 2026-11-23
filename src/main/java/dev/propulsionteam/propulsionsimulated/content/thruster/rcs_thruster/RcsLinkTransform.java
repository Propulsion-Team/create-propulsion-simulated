package dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class RcsLinkTransform extends ValueBoxTransform.Dual {
    private final int nozzle;

    public RcsLinkTransform(boolean first, int nozzle) {
        super(first);
        this.nozzle = nozzle;
    }

    @Override
    public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
        boolean single = ((RcsThrusterBlock) state.getBlock()).isSingle();
        Vector3d point;
        if (single) point = new Vector3d(isFirst() ? 11 : 5, 7, 4.49);
        else point = switch (nozzle) {
            case 0 -> new Vector3d(isFirst() ? 5 : 11, 2, 14.51);
            case 1 -> new Vector3d(isFirst() ? 11 : 5, 2, 1.49);
            case 2 -> new Vector3d(1.49, 2, isFirst() ? 5 : 11);
            default -> new Vector3d(14.51, 2, isFirst() ? 11 : 5);
        };
        point = RcsOrientation.point(point.div(16), RcsThrusterBlock.normal(state));
        return new Vec3(point.x, point.y, point.z);
    }

    @Override
    public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
        boolean single = ((RcsThrusterBlock) state.getBlock()).isSingle();
        Direction side = single ? Direction.NORTH : RcsOrientation.exhaust(false, nozzle);
        ms.mulPose(RcsOrientation.rotation(RcsThrusterBlock.normal(state)));
        ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(side) + 180));
    }

    @Override
    public float getScale() { return 0.4975f; }
}
