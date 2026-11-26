package dev.propulsionteam.propulsionsimulated.content.platinum;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
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
        // Not delegating to super: it briefly places a copper glass pipe, which makes
        // neighbouring platinum pipes disconnect.
        if (tryRemoveBracket(context)) {
            return InteractionResult.SUCCESS;
        }
        var level = context.getLevel();
        var pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();

        Direction.Axis axis = FluidPropagator.getStraightPipeAxis(state);
        if (axis == null) {
            Vec3 click = context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos));
            double closest = Double.MAX_VALUE;
            axis = Direction.Axis.Y;
            for (Direction direction : Direction.values()) {
                if (clickedFace.getAxis() == direction.getAxis()) {
                    continue;
                }
                double distance = Vec3.atCenterOf(direction.getNormal()).distanceToSqr(click);
                if (distance < closest) {
                    closest = distance;
                    axis = direction.getAxis();
                }
            }
        }

        if (clickedFace.getAxis() == axis) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            FluidTransportBehaviour.cacheFlows(level, pos);
            level.setBlockAndUpdate(pos, PropulsionBlocks.GLASS_PLATINUM_PIPE.get().defaultBlockState()
                .setValue(GlassFluidPipeBlock.AXIS, axis)
                .setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED)));
            FluidTransportBehaviour.loadFlows(level, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
