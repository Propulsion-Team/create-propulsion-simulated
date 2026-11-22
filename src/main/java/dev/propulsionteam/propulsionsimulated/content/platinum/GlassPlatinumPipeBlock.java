package dev.propulsionteam.propulsionsimulated.content.platinum;

import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public class GlassPlatinumPipeBlock extends GlassFluidPipeBlock {
    public GlassPlatinumPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends StraightPipeBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.GLASS_PLATINUM_PIPE_BLOCK_ENTITY.get();
    }

    @Override
    public BlockState toRegularPipe(LevelAccessor level, BlockPos pos, BlockState state) {
        var pipe = PropulsionBlocks.PLATINUM_PIPE.get();
        Direction side = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS));
        return pipe.updateBlockState(pipe.getAxisState(state.getValue(AXIS)), side, null, level, pos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(PropulsionBlocks.PLATINUM_PIPE.get());
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        return ItemRequirement.of(PropulsionBlocks.PLATINUM_PIPE.get().defaultBlockState(), be);
    }
}
