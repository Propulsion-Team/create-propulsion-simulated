package dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

public final class RcsShapes {
    private static final double[][] SINGLE = {
        {2, 0, 2, 14, 4, 14},
        {6, 10, 6, 10, 12, 10},
        {7, 9, 7, 9, 10, 9},
        {5.5, 12, 5.5, 10.5, 14, 10.5},
        {9, 5, 4, 13, 9, 5},
        {3, 5, 4, 7, 9, 5},
        {3, 4, 5, 13, 9, 11}
    };
    private static final double[][] FULL = {
        {2, 0, 2, 14, 4, 14},
        {4, 4, 5, 12, 10, 11},
        {5.5, 4.5, 14, 10.5, 9.5, 16},
        {7, 6, 11, 9, 8, 12},
        {6, 5, 12, 10, 9, 14},
        {6, 5, 2, 10, 9, 4},
        {7, 6, 4, 9, 8, 5},
        {5.5, 4.5, 0, 10.5, 9.5, 2},
        {3, 0, 14, 7, 4, 15},
        {9, 0, 14, 13, 4, 15},
        {9, 0, 1, 13, 4, 2},
        {3, 0, 1, 7, 4, 2},
        {14, 4.5, 5.5, 16, 9.5, 10.5},
        {12, 5, 6, 14, 9, 10},
        {2, 5, 6, 4, 9, 10},
        {0, 4.5, 5.5, 2, 9.5, 10.5},
        {14, 0, 3, 15, 4, 7},
        {14, 0, 9, 15, 4, 13},
        {1, 0, 3, 2, 4, 7},
        {1, 0, 9, 2, 4, 13}
    };

    private static final VoxelShape[][] SHAPES = new VoxelShape[2][6];

    static {
        for (int variant = 0; variant < 2; variant++) {
            for (Direction normal : Direction.values()) {
                VoxelShape shape = Shapes.empty();
                for (double[] box : variant == 0 ? SINGLE : FULL) {
                    Vector3d a = RcsOrientation.point(new Vector3d(box[0], box[1], box[2]).div(16), normal);
                    Vector3d b = RcsOrientation.point(new Vector3d(box[3], box[4], box[5]).div(16), normal);
                    shape = Shapes.or(shape, Shapes.box(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
                            Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z)));
                }
                SHAPES[variant][normal.ordinal()] = shape.optimize();
            }
        }
    }

    private RcsShapes() {}

    public static VoxelShape get(boolean single, Direction normal) {
        return SHAPES[single ? 0 : 1][normal.ordinal()];
    }
}
