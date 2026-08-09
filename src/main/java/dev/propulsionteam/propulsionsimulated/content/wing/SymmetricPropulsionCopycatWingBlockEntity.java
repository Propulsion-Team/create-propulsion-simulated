package dev.propulsionteam.propulsionsimulated.content.wing;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SymmetricPropulsionCopycatWingBlockEntity extends CopycatBlockEntity {
    public SymmetricPropulsionCopycatWingBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.SYMMETRIC_COPYCAT_WING_BLOCK_ENTITY.get(), pos, state);
    }
}

