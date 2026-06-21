package dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster;

import java.util.function.Consumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class CreativeThrusterVisual extends AbstractBlockEntityVisual<CreativeThrusterBlockEntity> {
    private OrientedInstance bracket;

    public CreativeThrusterVisual(VisualizationContext context, CreativeThrusterBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        Direction facing = blockState.getValue(CreativeThrusterBlock.FACING);
        Direction placementFacing = blockState.getValue(CreativeThrusterBlock.PLACEMENT_FACING);

        if (blockState.hasProperty(ThrusterBlock.MULTIBLOCK) && blockState.getValue(ThrusterBlock.MULTIBLOCK)) {
            bracket = null;
            return;
        }

        if (facing.getAxis() == placementFacing.getAxis()) {
            bracket = null;
            return;
        }

        bracket = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(PropulsionPartialModels.CREATIVE_THRUSTER_BRACKET)).createInstance();

        float angle = getBracketAngle(facing, placementFacing);
        Quaternionf q = new Quaternionf(facing.getRotation());
        q.mul(new Quaternionf().rotationX(Mth.DEG_TO_RAD * 90));
        q.mul(new Quaternionf().rotationZ(Mth.DEG_TO_RAD * angle));
        bracket.position(getVisualPosition())
            .rotation(q)
            .light(computePackedLight())
            .setChanged();
    }

    private float getBracketAngle(Direction facing, Direction placementFacing) {
        Vector3f local = new Vector3f(placementFacing.step());
        Quaternionf q = new Quaternionf(facing.getRotation());
        q.conjugate();
        local.rotate(q);
        local.rotateX((float) Math.toRadians(-90));
        double targetAngle = Math.toDegrees(Math.atan2(local.y, local.x));
        return (float) (targetAngle + 90);
    }

    @Override
    public void updateLight(float partialTick) {
        if (bracket != null) {
            bracket.light(computePackedLight()).setChanged();
        }
    }

    @Override
    protected void _delete() {
        if (bracket != null) {
            bracket.delete();
        }
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (bracket != null) {
            consumer.accept(bracket);
        }
    }
}
