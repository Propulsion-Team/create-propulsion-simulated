package dev.propulsionteam.propulsionsimulated.mixin;

import com.simibubi.create.content.fluids.FluidPropagator;
import dev.propulsionteam.propulsionsimulated.content.platinum.PipeFamily;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create ignores pipe-to-pipe neighbour changes, which separate pipe families never propagate between. */
@Mixin(FluidPropagator.class)
public class FluidPropagatorMixin {
    @Inject(method = "validateNeighbourChange", at = @At("HEAD"), cancellable = true)
    private static void createpropulsion$updateOnOtherFamilyPlaced(BlockState state, Level world, BlockPos pos,
                                                                  Block otherBlock, BlockPos neighborPos,
                                                                  boolean isMoving,
                                                                  CallbackInfoReturnable<Direction> cir) {
        if (!world.isClientSide && PipeFamily.conflict(state, world.getBlockState(neighborPos))) {
            for (Direction direction : Iterate.directions) {
                if (pos.relative(direction).equals(neighborPos)) {
                    cir.setReturnValue(direction);
                }
            }
        }
    }

    @Inject(method = "propagateChangedPipe", at = @At("HEAD"))
    private static void createpropulsion$updateOnOtherFamilyRemoved(LevelAccessor world, BlockPos pipePos,
                                                                   BlockState pipeState, CallbackInfo ci) {
        // Only on removal or replacement, otherwise neighbours would keep re-ticking each other
        if (world.getBlockState(pipePos).getBlock() == pipeState.getBlock()) {
            return;
        }
        for (Direction direction : Iterate.directions) {
            BlockPos neighbourPos = pipePos.relative(direction);
            BlockState neighbour = world.getBlockState(neighbourPos);
            if (PipeFamily.conflict(pipeState, neighbour)) {
                world.scheduleTick(neighbourPos, neighbour.getBlock(), 1);
            }
        }
    }
}
