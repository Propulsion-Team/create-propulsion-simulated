package dev.propulsionteam.propulsionsimulated.registries;

import net.createmod.catnip.math.VoxelShaper;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

import static net.minecraft.core.Direction.NORTH;

public class PropulsionShapes {
    public static final VoxelShaper
        THRUSTER = ShapeBuilder.shape()
            .add(Block.box(2, 2, 0, 14, 14, 2))
            .add(Block.box(1, 1, 2, 15, 15, 14))
            .add(Block.box(3, 3, 14, 13, 13, 16))
            .forDirectional(),

        VECTOR_THRUSTER = ShapeBuilder.shape()
            .add(Block.box(2, 2, 0, 14, 14, 4))
            .add(Block.box(1, 9, 0, 2, 13, 4))
            .add(Block.box(1, 3, 0, 2, 7, 4))
            .add(Block.box(14, 9, 0, 15, 13, 4))
            .add(Block.box(14, 3, 0, 15, 7, 4))
            .add(Block.box(3, 1, 0, 7, 2, 4))
            .add(Block.box(9, 1, 0, 13, 2, 4))
            .add(Block.box(3, 14, 0, 7, 15, 4))
            .add(Block.box(9, 14, 0, 13, 15, 4))
            .add(Block.box(4, 4, 4, 12, 12, 5))
            .forDirectional(),

        CREATIVE_VECTOR_THRUSTER = ShapeBuilder.shape()
            .add(Block.box(2, 2, 0, 14, 14, 4))
            .add(Block.box(1, 9, 0, 2, 13, 4))
            .add(Block.box(1, 3, 0, 2, 7, 4))
            .add(Block.box(14, 9, 0, 15, 13, 4))
            .add(Block.box(14, 3, 0, 15, 7, 4))
            .add(Block.box(3, 1, 0, 7, 2, 4))
            .add(Block.box(9, 1, 0, 13, 2, 4))
            .add(Block.box(3, 14, 0, 7, 15, 4))
            .add(Block.box(9, 14, 0, 13, 15, 4))
            .add(Block.box(4, 4, 4, 12, 12, 5))
            .forDirectional(),

        CREATIVE_THRUSTER = ShapeBuilder.shape()
            .add(Block.box(3, 3, 0, 13, 13, 10))
            .add(Block.box(4, 4, 10, 12, 12, 12))
            .forDirectional(),

        INLINE_OPTICAL_SENSOR = ShapeBuilder.shape()
            .add(Block.box(4, 4, 10, 12, 12, 16))
            .forDirectional(),

        OPTICAL_SENSOR = ShapeBuilder.shape()
            .add(Block.box(0, 0, 4, 16, 16, 16))
            .add(Block.box(4, 4, 0, 12, 12, 4))
            .forDirectional(),

        LODESTONE_TRACKER = ShapeBuilder.shape()
            .add(Block.box(1, 0, 1, 15, 2, 15))
            .add(Block.box(2, 2, 2, 14, 9, 14))
            .add(Block.box(0, 9, 0, 16, 14, 16))
            .forDirectional(),

        PHYSICS_ASSEMBLER = ShapeBuilder.shape()
            .add(Block.box(0, 0, 0, 16, 14, 16))
            .add(Block.box(1, 14, 1, 15, 16, 15))
            .forDirectional(),

        CORAL_GENERATOR = ShapeBuilder.shape()
            .add(Block.box(1, 0, 1, 15, 16, 15))
            .add(Block.box(0, 1, 0, 16, 15, 16))
            .forDirectional(Direction.UP),
        
        PROPELLER = ShapeBuilder.shape()
            .add(Block.box(0, 0, 4, 16, 16, 16))
            .forDirectional(),

        STIRLING_ENGINE = ShapeBuilder.shape()
            .add(Block.box(0, 0, 0, 16, 3, 16))
            .add(Block.box(0, 3, 0, 16, 5, 14))
            .add(Block.box(2, 5, 0, 14, 13, 14))
            .forDirectional(Direction.SOUTH),

        TILT_ADAPTER = ShapeBuilder.shape()
            .add(Block.box(0, 0, 3, 16, 16, 16))
            .forDirectional(),

        ADVANCED_TILT_ADAPTER = ShapeBuilder.shape()
            .add(Block.box(0, 0, 0, 16, 16, 16))
            .forDirectional(),

        WING = getWingShape(4),
        WING_8 = getWingShape(8),
        WING_12 = getWingShape(12),

        SYMMETRIC_WING = getSymmetricWingShape(4);

    public static VoxelShaper getWingShape(int width) {
        if (width % 2 != 0) throw new IllegalArgumentException("Humble odd width exception");
        int halfWidth = width / 2;
        return ShapeBuilder.shape()
            .add(Block.box(0, 8 - halfWidth, 0, 16, 8 + halfWidth, 16))
            .forDirectional(Direction.UP);
    }

    public static VoxelShaper getSymmetricWingShape(int width) {
        if (width % 2 != 0) throw new IllegalArgumentException("Humble odd width exception");
        int halfWidth = width / 2;
        return ShapeBuilder.shape()
            .add(Block.box(0, 8 - halfWidth, 0, 16, 8 + halfWidth, 16))
            .forAxis();
    }
    
    public static class ShapeBuilder {
        private VoxelShape shape;
    
        public static ShapeBuilder shapeBuilder(VoxelShape shape) {
            return new ShapeBuilder(shape);
        }
    
        public static ShapeBuilder shape() {
            return new ShapeBuilder(Shapes.empty());
        }
    
        private static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
            return Block.box(x1, y1, z1, x2, y2, z2);
        }
    
        public ShapeBuilder(VoxelShape shape) {
            this.shape = shape;
        }
    
        public ShapeBuilder add(VoxelShape shape) {
            this.shape = Shapes.or(this.shape, shape);
            return this;
        }
    
        public ShapeBuilder add(double x1, double y1, double z1, double x2, double y2, double z2) {
            return add(cuboid(x1, y1, z1, x2, y2, z2));
        }
    
        public ShapeBuilder erase(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.shape = Shapes.join(shape, cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
            return this;
        }
    
        public VoxelShape build() {
            return shape;
        }
    
        public VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
            return factory.apply(shape, direction);
        }
    
        public VoxelShaper build(BiFunction<VoxelShape, Axis, VoxelShaper> factory, Axis axis) {
            return factory.apply(shape, axis);
        }
    
        public VoxelShaper forDirectional(Direction direction) {
            return build(VoxelShaper::forDirectional, direction);
        }
    
        public VoxelShaper forAxis() {
            return build(VoxelShaper::forAxis, Axis.Y);
        }
    
        public VoxelShaper forHorizontalAxis() {
            return build(VoxelShaper::forHorizontalAxis, Axis.Z);
        }
    
        public VoxelShaper forHorizontal(Direction direction) {
            return build(VoxelShaper::forHorizontal, direction);
        }
    
        public VoxelShaper forDirectional() {
            return forDirectional(NORTH);
        }
    }
}
