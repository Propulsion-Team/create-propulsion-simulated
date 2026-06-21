package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster;

import com.mojang.serialization.MapCodec;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlock;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nonnull;

public class LiquidVectorThrusterBlock extends VectorThrusterBlock {
    public static final MapCodec<LiquidVectorThrusterBlock> CODEC = simpleCodec(LiquidVectorThrusterBlock::new);

    public LiquidVectorThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }


    @Override
    @SuppressWarnings("unchecked")
    public Class<AbstractThrusterBlockEntity> getBlockEntityClass() {
        return (Class<AbstractThrusterBlockEntity>) (Object) LiquidVectorThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LiquidVectorThrusterBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.LIQUID_VECTOR_THRUSTER_BLOCK_ENTITY.get();
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.LIQUID_VECTOR_THRUSTER_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}
