package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterPhysics;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import javax.annotation.Nullable;

public final class ThrusterForceProvider {
    private ThrusterForceProvider() {
    }

    @Nullable
    public static ForceSample createSample(final AbstractThrusterBlockEntity blockEntity, final double timeStep) {
        final BlockPos controller = blockEntity.controllerPos;
        if (controller != null) return null;
        final ForceSample sample = createSample(blockEntity.getBlockPos(), controller, blockEntity.width,
            blockEntity.getThrustDirectionLocal(), blockEntity.getCurrentThrust(), timeStep,
            PropulsionConfig.NOZZLE_OFFSET_FROM_CENTER.get());
        if (sample == null || !(blockEntity instanceof VectorThrusterBlockEntity)
                || !PropulsionConfig.VECTOR_THRUSTER_GEOMETRY_AWARE_TORQUE.get()) return sample;
        final Vector3d point = VectorThrusterPhysics.applicationPoint(
                blockEntity.getBlockPos(), blockEntity.getFacing(), sample.directionLocal());
        return point.isFinite() ? new ForceSample(point, sample.impulseLocal(), sample.directionLocal()) : null;
    }

    @Nullable
    static ForceSample createSample(final BlockPos blockPos, @Nullable final BlockPos controller,
                                    final int width, final Vector3d direction, final double thrust,
                                    final double timeStep, final double nozzleOffset) {
        if (controller != null || width < 1 || !direction.isFinite()
            || direction.lengthSquared() == 0 || !Double.isFinite(thrust) || thrust <= 0
            || !Double.isFinite(timeStep) || timeStep <= 0 || !Double.isFinite(nozzleOffset)) return null;
        final Vector3d directionLocal = new Vector3d(direction).normalize();
        final double offset = width / 2.0d;

        final Vector3d applicationPoint = new Vector3d(
                blockPos.getX() + offset,
                blockPos.getY() + offset,
                blockPos.getZ() + offset
        ).fma(nozzleOffset, directionLocal);

        final Vector3d impulseLocal = new Vector3d(directionLocal).mul(thrust * timeStep);

        return new ForceSample(applicationPoint, impulseLocal, directionLocal);
    }

    public record ForceSample(Vector3d pointLocal, Vector3d impulseLocal, Vector3d directionLocal) {
    }
}
