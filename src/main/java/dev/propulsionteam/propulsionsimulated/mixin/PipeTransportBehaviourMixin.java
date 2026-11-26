package dev.propulsionteam.propulsionsimulated.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import dev.propulsionteam.propulsionsimulated.content.platinum.PipeFamily;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Blocks fluid flow between pipe families. Straight pipes can be open towards
 * each other even though the block states never chose to connect.
 */
@Mixin(targets = {
    "com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity$StandardPipeFluidTransportBehaviour",
    "com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity$StraightPipeFluidTransportBehaviour"
})
public class PipeTransportBehaviourMixin {
    @ModifyReturnValue(method = "canHaveFlowToward", at = @At("RETURN"))
    private boolean createpropulsion$separatePipeFamilies(boolean original, BlockState state, Direction direction) {
        var blockEntity = ((FluidTransportBehaviour) (Object) this).blockEntity;
        return original && !PipeFamily.conflictsWithNeighbour(
            blockEntity.getLevel(), blockEntity.getBlockPos(), state, direction);
    }
}
