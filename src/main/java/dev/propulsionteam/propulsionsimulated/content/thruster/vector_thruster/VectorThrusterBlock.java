package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import com.mojang.serialization.MapCodec;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterShapes;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.DirectionalBlock;

public class VectorThrusterBlock extends IonThrusterBlock {
    public static final MapCodec<VectorThrusterBlock> CODEC = simpleCodec(VectorThrusterBlock::new);

    public VectorThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (facing == Direction.UP) {
            return ThrusterShapes.VECTOR_THRUSTER.get(Direction.DOWN);
        } else if (facing == Direction.DOWN) {
            return ThrusterShapes.VECTOR_THRUSTER.get(Direction.UP);
        }
        return ThrusterShapes.VECTOR_THRUSTER.get(facing);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<AbstractThrusterBlockEntity> getBlockEntityClass() {
        return (Class<AbstractThrusterBlockEntity>) (Object) VectorThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AbstractThrusterBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.ION_THRUSTER_BLOCK_ENTITY.get();
    }
}
