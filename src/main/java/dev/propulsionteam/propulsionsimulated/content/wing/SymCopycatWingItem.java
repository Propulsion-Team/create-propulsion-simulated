package dev.propulsionteam.propulsionsimulated.content.wing;

import javax.annotation.Nonnull;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SymCopycatWingItem extends BlockItem {
    public SymCopycatWingItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState clickedState = world.getBlockState(pos);

        if (player != null && player.isShiftKeyDown() && clickedState.getBlock() instanceof SymCopycatWingBlock) {
            SymCopycatWingBlock clickedWing = (SymCopycatWingBlock) clickedState.getBlock();
            if (clickedWing.getWidth() != 12) {
                BlockState targetState = (clickedWing.getWidth() == 4)
                    ? PropulsionBlocks.SYMMETRIC_COPYCAT_WING_8.get().defaultBlockState()
                    : PropulsionBlocks.SYMMETRIC_COPYCAT_WING_12.get().defaultBlockState();
                
                targetState = targetState.setValue(SymCopycatWingBlock.AXIS, clickedState.getValue(SymCopycatWingBlock.AXIS));
                if (!world.isClientSide()) {
                    CopycatBlockEntity oldCopycat = null;
                    BlockEntity oldBE = world.getBlockEntity(pos);
                    if (oldBE instanceof CopycatBlockEntity) {
                        oldCopycat = (CopycatBlockEntity) oldBE;
                    }

                    world.setBlock(pos, targetState, 3);

                    // Retain copycat material safely without copying full BE metadata between variants.
                    BlockEntity newBE = world.getBlockEntity(pos);
                    if (oldCopycat != null && newBE instanceof CopycatBlockEntity newCopycat) {
                        newCopycat.setMaterial(oldCopycat.getMaterial());
                        newCopycat.setConsumedItem(oldCopycat.getConsumedItem());
                    }
                    
                    if (!player.getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                }
                
                world.playSound(player, pos, targetState.getSoundType(world, pos, player).getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.sidedSuccess(world.isClientSide());
            }
        }
        return super.useOn(context);
    }

}
