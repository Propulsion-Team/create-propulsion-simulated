package dev.propulsionteam.propulsionsimulated.content.heat.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import dev.propulsionteam.propulsionsimulated.utility.math.MathUtility;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class StirlingEngineVisual extends KineticBlockEntityVisual<StirlingEngineBlockEntity> implements SimpleDynamicVisual {
    protected final RotatingInstance shaft;
    protected final List<TransformedInstance> pistons = new ArrayList<>(4);
    
    private final static int[] offsetArray = {0, 7, 2, 9};
    private final Direction facing;
    private final Vector3f center = new Vector3f(0.5f, 0.5f, 0.5f);

    public StirlingEngineVisual(VisualizationContext context, StirlingEngineBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        this.facing = blockState.getValue(StirlingEngineBlock.HORIZONTAL_FACING);

        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF)).createInstance();

        shaft.setup(blockEntity)
             .setPosition(getVisualPosition()) 
             .rotateToFace(Direction.SOUTH, facing)
             .setChanged();

        var pistonModel = Models.partial(PropulsionPartialModels.STIRLING_ENGINE_PISTON);
        
        for (int i = 0; i < 4; i++) {
            TransformedInstance piston = instancerProvider().instancer(InstanceTypes.TRANSFORMED, pistonModel).createInstance();
            pistons.add(piston);
        }
        
        animate(partialTick);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        animate(ctx.partialTick());
    }

    private void animate(float partialTick) {
        float time = AnimationTickHolder.getRenderTime(blockEntity.getLevel());
        float timeSeconds = time / 20.0f;
        
        float speed = Math.abs(blockEntity.getSpeed() / StirlingEngineBlockEntity.MAX_GENERATED_RPM);
        float effectiveRevolutionPeriod = Float.MAX_VALUE;
        if (speed > MathUtility.epsilon) {
            effectiveRevolutionPeriod = PropulsionConfig.STIRLING_REVOLUTION_PERIOD.get().floatValue() / speed;
        }

        float crankRadius = PropulsionConfig.STIRLING_CRANK_RADIUS.get().floatValue();
        float conrodLength = PropulsionConfig.STIRLING_CONROD_LENGTH.get().floatValue();

        Vector4f normalizedExtensions = StirlingEngineRenderer.calculateExtensions(timeSeconds, crankRadius, conrodLength, effectiveRevolutionPeriod);

        final float offsetDistance = 2 / 16.0f;
        for (int i = 0; i < 4; i++) {
            float normalized;
            if (i == 0) normalized = normalizedExtensions.x;
            else if (i == 1) normalized = normalizedExtensions.y;
            else if (i == 2) normalized = normalizedExtensions.z;
            else normalized = normalizedExtensions.w;

            float offset = Math.min(offsetDistance - 0.001f, normalized * offsetDistance); 

            transformPiston(pistons.get(i), i, offset);
        }
    }

    private void transformPiston(TransformedInstance instance, int index, float extensionOffset) {
        Quaternionf rotation = new Quaternionf();
        rotation.mul(facing.getRotation());
        
        if (index >= 2) {
            rotation.mul(Axis.ZP.rotationDegrees(180));
        }
        rotation.mul(Axis.XP.rotationDegrees(270));

        Vector3f localOffset = new Vector3f(extensionOffset, 0, offsetArray[index] / 16.0f);
        
        Vector3f relativePos = new Vector3f(localOffset);
        relativePos.sub(center); 
        relativePos.rotate(rotation); 
        
        Vector3f finalPos = new Vector3f(center);
        finalPos.add(relativePos);
        
        BlockPos visualPos = getVisualPosition();
        finalPos.add(visualPos.getX(), visualPos.getY(), visualPos.getZ());

        float fixX = 0;
        float fixZ = 0;
        
        boolean isGroupA = index < 2;
        boolean isGroupB = index >= 2;

        switch (facing) {
            case NORTH:
                if (isGroupA) {
                    fixX = -1;
                    fixZ = -1;
                }
                break;
            case SOUTH:
                if (isGroupB) {
                    fixX = -1;
                    fixZ = -1;
                }
                break;
            case EAST:
                if (isGroupB) fixX = -1;
                if (isGroupA) fixZ = -1;
                break;
            case WEST:
                if (isGroupB) fixZ = -1;
                if (isGroupA) fixX = -1;
                break;
            default:
                break;
        }

        finalPos.add(fixX, 0, fixZ);

        instance.setIdentityTransform().translate(finalPos).rotateCentered(rotation).setChanged();
    }

    @Override
    public void update(float pt) {
        shaft.setup(blockEntity).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(shaft);
        for (TransformedInstance piston : pistons) {
            relight(piston);
        }
    }

    @Override
    protected void _delete() {
        shaft.delete();
        for (TransformedInstance piston : pistons) {
            piston.delete();
        }
        pistons.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
        for (TransformedInstance piston : pistons) {
            consumer.accept(piston);
        }
    }
}
