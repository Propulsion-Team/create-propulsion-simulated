package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TiltAdapterBlock extends AbstractTiltAdapterBlock<TiltAdapterBlockEntity> {

    public TiltAdapterBlock(Properties properties) {
        super(properties, TiltAdapterBlockEntity.class,
            PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY::get,
            TiltAdapterBlockEntity::new);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }
}
