package dev.propulsionteam.propulsionsimulated.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import dev.propulsionteam.propulsionsimulated.content.platinum.PipeFamily;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidPipeBlock.class)
public class FluidPipeBlockMixin {
    @WrapOperation(method = "updateBlockState", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/fluids/pipes/FluidPipeBlock;canConnectTo(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
    private boolean createpropulsion$separatePipeFamilies(BlockAndTintGetter world, BlockPos neighbourPos,
                                                         BlockState neighbour, Direction direction,
                                                         Operation<Boolean> original,
                                                         @Local(argsOnly = true) BlockState state) {
        return !PipeFamily.conflict(state, neighbour)
            && original.call(world, neighbourPos, neighbour, direction);
    }
}
