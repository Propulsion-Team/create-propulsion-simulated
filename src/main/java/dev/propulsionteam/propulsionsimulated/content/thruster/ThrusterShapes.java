package dev.propulsionteam.propulsionsimulated.content.thruster;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

import static net.minecraft.core.Direction.NORTH;

public final class ThrusterShapes {
    public static final VoxelShaper THRUSTER = ShapeBuilder.shape()
            .add(Block.box(2, 2, 0, 14, 14, 2))
            .add(Block.box(1, 1, 2, 15, 15, 14))
            .add(Block.box(3, 3, 14, 13, 13, 16))
            .forDirectional();

    public static final VoxelShaper CREATIVE_THRUSTER = ShapeBuilder.shape()
            .add(Block.box(3, 3, 0, 13, 13, 10))
            .add(Block.box(4, 4, 10, 12, 12, 12))
            .forDirectional();

    public static final VoxelShaper ION_THRUSTER = ShapeBuilder.shape()
            .add(Block.box(0, 0, 0, 16, 8, 16))
            .add(Block.box(0, 8, 5, 16, 12, 11))
            .add(Block.box(5, 8, 0, 11, 12, 16))
            .add(Block.box(3, 8, 3, 13, 14, 13))
            .add(Block.box(2, 14, 2, 14, 16, 14))
            .forDirectional(Direction.UP);

    /** Solid thruster in the same north-facing placement basis as the normal thruster. */
    public static final VoxelShaper SOLID_FUEL_THRUSTER = ShapeBuilder.shape()
            .add(Block.box(4, 4, 0, 12, 12, 1))        // fuel funnel
            .add(Block.box(0, 0, 0, 16, 16, 8))        // hull
            .add(Block.box(2, 2, 8, 14, 14, 10))       // nozzle neck
            .add(Block.box(0, 0, 10, 16, 16, 13))      // inner nozzle
            .add(Block.box(0, 0, 13, 16, 16, 16))      // outer nozzle, clipped to block bounds
            .forDirectional();

    public static final VoxelShaper VECTOR_THRUSTER = ShapeBuilder.shape()
            .add(Block.box(2, 2, 0, 14, 14, 4)) // input
            .add(Block.box(1, 9, 0, 2, 13, 4))  // west upper
            .add(Block.box(1, 3, 0, 2, 7, 4))   // west lower
            .add(Block.box(14, 9, 0, 15, 13, 4)) // east upper
            .add(Block.box(14, 3, 0, 15, 7, 4)) // east lower
            .add(Block.box(3, 1, 0, 7, 2, 4))   // down left
            .add(Block.box(9, 1, 0, 13, 2, 4))  // down right
            .add(Block.box(3, 14, 0, 7, 15, 4)) // up left
            .add(Block.box(9, 14, 0, 13, 15, 4)) // up right
            .add(Block.box(4, 4, 4, 12, 12, 5)) // mover
            .forDirectional();

    private ThrusterShapes() {
    }

    public static final class ShapeBuilder {
        private VoxelShape shape;

        private ShapeBuilder(final VoxelShape shape) {
            this.shape = shape;
        }

        public static ShapeBuilder shape() {
            return new ShapeBuilder(Shapes.empty());
        }

        public ShapeBuilder add(final VoxelShape shape) {
            this.shape = Shapes.or(this.shape, shape);
            return this;
        }

        public ShapeBuilder erase(final double x1,
                                  final double y1,
                                  final double z1,
                                  final double x2,
                                  final double y2,
                                  final double z2) {
            this.shape = Shapes.join(this.shape, Block.box(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
            return this;
        }

        public VoxelShape build() {
            return this.shape;
        }

        public VoxelShaper build(final BiFunction<VoxelShape, Direction, VoxelShaper> factory, final Direction direction) {
            return factory.apply(this.shape, direction);
        }

        public VoxelShaper forDirectional(final Direction direction) {
            return this.build(VoxelShaper::forDirectional, direction);
        }

        public VoxelShaper forDirectional() {
            return this.forDirectional(NORTH);
        }
    }
}
