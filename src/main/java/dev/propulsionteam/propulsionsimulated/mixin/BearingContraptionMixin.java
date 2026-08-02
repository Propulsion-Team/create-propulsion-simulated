package dev.propulsionteam.propulsionsimulated.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import dev.propulsionteam.propulsionsimulated.content.wing.CopycatWingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BearingContraption.class)
public class BearingContraptionMixin {
    @ModifyExpressionValue(
        method = "addBlock",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/AllTags$AllBlockTags;matches(Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean createpropulsion$recognizeCopycatWingsAsSails(
        boolean original,
        Level level,
        BlockPos pos,
        Pair<StructureBlockInfo, BlockEntity> capture
    ) {
        BlockState capturedState = capture.getLeft().state();
        return original || capturedState.getBlock() instanceof CopycatWingBlock;
    }
}
