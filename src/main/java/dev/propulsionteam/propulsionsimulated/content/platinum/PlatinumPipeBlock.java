package dev.propulsionteam.propulsionsimulated.content.platinum;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class PlatinumPipeBlock extends FluidPipeBlock {
    public PlatinumPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.PLATINUM_PIPE_BLOCK_ENTITY.get();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        InteractionResult result = super.onWrenched(state, context);
        var level = context.getLevel();
        var pos = context.getClickedPos();
        BlockState updated = level.getBlockState(pos);
        if (!level.isClientSide && updated.getBlock() instanceof GlassFluidPipeBlock
            && !(updated.getBlock() instanceof GlassPlatinumPipeBlock)) {
            FluidTransportBehaviour.cacheFlows(level, pos);
            level.setBlockAndUpdate(pos, PropulsionBlocks.GLASS_PLATINUM_PIPE.get().defaultBlockState()
                .setValue(GlassFluidPipeBlock.AXIS, updated.getValue(GlassFluidPipeBlock.AXIS))
                .setValue(BlockStateProperties.WATERLOGGED, updated.getValue(BlockStateProperties.WATERLOGGED)));
            FluidTransportBehaviour.loadFlows(level, pos);
        }
        return result;
    }
}
