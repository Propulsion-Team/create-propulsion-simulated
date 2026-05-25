package dev.propulsionteam.propulsionsimulated.content.thruster;

import net.createmod.catnip.math.VecHelper;
import dev.propulsionteam.propulsionsimulated.content.thruster.creative_thruster.CreativeThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Maps model-space nozzle openings (voxel coords) into world block space (0–1 from block min corner),
 * using the same centered rotations as {@link net.createmod.catnip.math.VecHelper} /
 * blockstate variants.
 */
public final class ThrusterPlumeAnchors {
    private ThrusterPlumeAnchors() {
    }

    /** Offset from block center (0.5, 0.5, 0.5) for BER pose stack. */
    public static Vec3 offsetFromBlockCenter(AbstractThrusterBlockEntity blockEntity) {
        Vec3 model = modelNozzleOpening(blockEntity);
        Vec3 world = rotateModelToWorld(model, blockEntity.getFacing(), layout(blockEntity));
        return world.subtract(0.5, 0.5, 0.5);
    }

    private static Vec3 modelNozzleOpening(AbstractThrusterBlockEntity blockEntity) {
        if (blockEntity instanceof SolidFuelThrusterBlockEntity) {
            return voxel(13, 8, 8);
        }
        if (blockEntity instanceof IonThrusterBlockEntity) {
            return voxel(8, 1, 8);
        }
        if (blockEntity instanceof CreativeThrusterBlockEntity) {
            return voxel(8, 8, 10);
        }
        // Standard / fluid thruster — nozzle mouth at low +Z in model file
        return voxel(8, 8, 14);
    }

    private static Vec3 voxel(float x, float y, float z) {
        return new Vec3(x / 16.0, y / 16.0, z / 16.0);
    }

    private static Layout layout(AbstractThrusterBlockEntity blockEntity) {
        if (blockEntity instanceof SolidFuelThrusterBlockEntity) {
            return Layout.SOLID_FUEL;
        }
        if (blockEntity instanceof IonThrusterBlockEntity) {
            return Layout.ION;
        }
        return Layout.THRUSTER;
    }

    private static Vec3 rotateModelToWorld(Vec3 point, Direction facing, Layout layout) {
        return switch (layout) {
            case THRUSTER -> rotateThruster(point, facing);
            case SOLID_FUEL -> rotateSolidFuel(point, facing);
            case ION -> rotateIon(point, facing);
        };
    }

    /** Same as {@code VectorThrusterBlockEntity.VectorThrusterLinkTransform#rotatePointForFacing}. */
    private static Vec3 rotateThruster(Vec3 vec, Direction facing) {
        return switch (facing) {
            case NORTH -> vec;
            case EAST -> VecHelper.rotateCentered(vec, -90, Direction.Axis.Y);
            case SOUTH -> VecHelper.rotateCentered(vec, 180, Direction.Axis.Y);
            case WEST -> VecHelper.rotateCentered(vec, 90, Direction.Axis.Y);
            case UP -> VecHelper.rotateCentered(vec, 90, Direction.Axis.X);
            case DOWN -> VecHelper.rotateCentered(vec, -90, Direction.Axis.X);
        };
    }

    /** Matches {@code assets/.../blockstates/solid_fuel_thruster.json}. */
    private static Vec3 rotateSolidFuel(Vec3 vec, Direction facing) {
        return switch (facing) {
            case WEST -> vec;
            case EAST -> VecHelper.rotateCentered(vec, 180, Direction.Axis.Y);
            case NORTH -> VecHelper.rotateCentered(vec, 90, Direction.Axis.Y);
            case SOUTH -> VecHelper.rotateCentered(vec, -90, Direction.Axis.Y);
            case UP -> VecHelper.rotateCentered(
                    VecHelper.rotateCentered(vec, -90, Direction.Axis.X),
                    90, Direction.Axis.Y);
            case DOWN -> VecHelper.rotateCentered(
                    VecHelper.rotateCentered(vec, 90, Direction.Axis.X),
                    90, Direction.Axis.Y);
        };
    }

    /** Matches {@code assets/.../blockstates/ion_thruster.json}. */
    private static Vec3 rotateIon(Vec3 vec, Direction facing) {
        return switch (facing) {
            case UP -> vec;
            case DOWN -> VecHelper.rotateCentered(vec, 180, Direction.Axis.X);
            case NORTH -> VecHelper.rotateCentered(vec, 90, Direction.Axis.X);
            case SOUTH -> VecHelper.rotateCentered(
                    VecHelper.rotateCentered(vec, 180, Direction.Axis.Y),
                    90, Direction.Axis.X);
            case EAST -> VecHelper.rotateCentered(
                    VecHelper.rotateCentered(vec, 90, Direction.Axis.Y),
                    90, Direction.Axis.X);
            case WEST -> VecHelper.rotateCentered(
                    VecHelper.rotateCentered(vec, -90, Direction.Axis.Y),
                    90, Direction.Axis.X);
        };
    }

    private enum Layout {
        THRUSTER,
        SOLID_FUEL,
        ION
    }
}
