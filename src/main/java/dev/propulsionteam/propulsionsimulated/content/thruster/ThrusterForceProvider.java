package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;

public final class ThrusterForceProvider {
    private ThrusterForceProvider() {
    }

    public static ForceSample createSample(final AbstractThrusterBlockEntity blockEntity, final double timeStep) {
        final Vector3d directionLocal = new Vector3d(blockEntity.getThrustDirectionLocal()).normalize();
        
        final BlockPos thrusterCenter = blockEntity.isController() ? blockEntity.getBlockPos() : blockEntity.controllerPos;
        final double thrusterWidth = blockEntity.isMultiblock() ? blockEntity.width : 1.0d;
        final double offset = thrusterWidth / 2.0d;

        final Vector3d applicationPoint = new Vector3d(
                thrusterCenter.getX() + offset,
                thrusterCenter.getY() + offset,
                thrusterCenter.getZ() + offset
        ).fma(PropulsionConfig.NOZZLE_OFFSET_FROM_CENTER.get(), directionLocal);

        final Vector3d impulseLocal = new Vector3d(directionLocal).mul(blockEntity.getCurrentThrust() * timeStep);

        return new ForceSample(applicationPoint, impulseLocal, directionLocal);
    }

    public record ForceSample(Vector3d pointLocal, Vector3d impulseLocal, Vector3d directionLocal) {
    }
}

