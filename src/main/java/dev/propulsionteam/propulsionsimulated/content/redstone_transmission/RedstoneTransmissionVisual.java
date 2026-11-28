package dev.propulsionteam.propulsionsimulated.content.redstone_transmission;

import dev.propulsionteam.propulsionsimulated.content.redstone_transmission.RedstoneTransmissionBlockEntity.TransmissionMode;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static dev.propulsionteam.propulsionsimulated.content.redstone_transmission.RedstoneTransmissionBlock.HORIZONTAL_FACING;
import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;

public class RedstoneTransmissionVisual extends KineticBlockEntityVisual<RedstoneTransmissionBlockEntity> implements SimpleDynamicVisual {
    private final List<RotatingInstance> shaftInstances = new ArrayList<>();
    private TransformedInstance minus;
    private TransformedInstance plus;
    private TransformedInstance hand;

    private PoseStack ms;

    public RedstoneTransmissionVisual(VisualizationContext modelManager, RedstoneTransmissionBlockEntity blockEntity, float partialTick) {
        super(modelManager, blockEntity, partialTick);

        Direction facing = blockEntity.getBlockState().getValue(HORIZONTAL_FACING);
        Direction.Axis axis = blockEntity.getBlockState().getValue(AXIS);

        //Shafts
        for (Direction direction : Iterate.directionsInAxis(axis)) {
            RotatingInstance instance = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance();
            
            instance.setup(blockEntity)
                .setPosition(getVisualPosition())
                .setRotationAxis(Direction.Axis.Z)
                .rotateToFace(Direction.SOUTH, direction)
                .setChanged();
                    
            shaftInstances.add(instance);
        }

        //Plus/minus/gauge
        minus = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(PropulsionPartialModels.TRANSMISSION_MINUS)).createInstance();
        plus = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(PropulsionPartialModels.TRANSMISSION_PLUS)).createInstance();
        hand = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.GAUGE_DIAL)).createInstance();

        ms = new PoseStack();
        var msr = TransformStack.of(ms);
        msr.translate(getVisualPosition());
        msr.pushPose();

        msr.rotateCenteredDegrees(-facing.toYRot() - 90, Direction.UP);
        minus.translate(getVisualPosition()).rotateCenteredDegrees(-facing.toYRot(), Direction.UP);
        plus.translate(getVisualPosition()).rotateCenteredDegrees(-facing.toYRot(), Direction.UP);

        if(blockEntity.getBlockState().getValue(AXIS).isHorizontal()) {
            minus.rotateCenteredDegrees(90, Direction.EAST);
            plus.rotateCenteredDegrees(90, Direction.EAST);
            msr.rotateCenteredDegrees(90, Direction.Axis.Z);
        }

        msr.translate(2f / 16, 0, 0);

        minus.setChanged();
        plus.setChanged();
        hand.setTransform(ms).setChanged();

        msr.popPose();
    }

    @Override
    protected void _delete() {
        shaftInstances.forEach(RotatingInstance::delete);
        minus.delete();
        plus.delete();
        hand.delete();
    }

    @Override
    public void updateLight(float partialTick) {
        shaftInstances.forEach(this::relight);
        relight(minus, plus, hand);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        shaftInstances.forEach(consumer::accept);
        consumer.accept(minus);
        consumer.accept(plus);
        consumer.accept(hand);
    }

    @Override
    public void beginFrame(Context context) {
        Direction facing = blockEntity.getBlockState().getValue(HORIZONTAL_FACING);
        Direction.Axis axis = blockEntity.getBlockState().getValue(AXIS);
        //Shafts
        int instanceIndex = 0;

        for (Direction direction : Iterate.directionsInAxis(axis)) {
            if (instanceIndex >= shaftInstances.size()) break;

            RotatingInstance instance = shaftInstances.get(instanceIndex);
            float speedToApply = RedstoneTransmissionRenderer.getDirectionalSpeed(blockEntity, direction);
            
            instance.setup(blockEntity, speedToApply).setChanged();
            instanceIndex++;
        }

        //Plus/minus/gauge
        int shift_up = blockEntity.get_shift_up();
        int shift_down = blockEntity.get_shift_down();
        //In direct mode both plus and minus sides control the same thing, so they should have the same redstone tint
        if (blockEntity.controlMode.get() == TransmissionMode.DIRECT) {
            int max_shift = Math.max(shift_up, shift_down);
            shift_up = max_shift;
            shift_down = max_shift;
        }

        Color up_color = new Color(Color.mixColors(0x470102, 0xCD0000, shift_up / 15f));
        Color down_color = new Color(Color.mixColors(0x470102, 0xCD0000, shift_down / 15f));

        minus.color(down_color.getRed(), down_color.getGreen(), down_color.getBlue()).setChanged();
        plus.color(up_color.getRed(), up_color.getGreen(), up_color.getBlue()).setChanged();

        var msr = TransformStack.of(ms);
        msr.pushPose();

        float dialPivot = 5.75f / 16;

        msr.rotateCenteredDegrees(-facing.toYRot() - 90, Direction.UP);
        if(blockEntity.getBlockState().getValue(AXIS).isHorizontal()) {
            msr.rotateCenteredDegrees(90, Direction.Axis.Z);
        }
        msr.translate(2f / 16, dialPivot, dialPivot)
            .rotate(blockEntity.getGaugeTarget(context.partialTick()), Direction.EAST)
            .translate(0, -dialPivot, -dialPivot);;

        hand.setTransform(ms).setChanged();

        msr.popPose();
    }
}


