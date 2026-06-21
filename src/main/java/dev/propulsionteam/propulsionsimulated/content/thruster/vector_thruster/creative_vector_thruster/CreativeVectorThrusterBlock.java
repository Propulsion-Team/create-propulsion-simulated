package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlock;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionShapes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreativeVectorThrusterBlock extends VectorThrusterBlock implements IWrenchable {
    public static final MapCodec<CreativeVectorThrusterBlock> CODEC = simpleCodec(CreativeVectorThrusterBlock::new);

    public CreativeVectorThrusterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends net.minecraft.world.level.block.DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<AbstractThrusterBlockEntity> getBlockEntityClass() {
        return (Class<AbstractThrusterBlockEntity>) (Object) CreativeVectorThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AbstractThrusterBlockEntity> getBlockEntityType() {
        return PropulsionBlockEntities.CREATIVE_VECTOR_THRUSTER_BLOCK_ENTITY.get();
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CreativeVectorThrusterBlockEntity(pos, state);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
            if (be instanceof CreativeVectorThrusterBlockEntity creativeVector) {
                creativeVector.cyclePlumeType();
                IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        if (facing == Direction.UP) {
            return PropulsionShapes.CREATIVE_VECTOR_THRUSTER.get(Direction.DOWN);
        } else if (facing == Direction.DOWN) {
            return PropulsionShapes.CREATIVE_VECTOR_THRUSTER.get(Direction.UP);
        }
        return PropulsionShapes.CREATIVE_VECTOR_THRUSTER.get(facing);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (type == PropulsionBlockEntities.CREATIVE_VECTOR_THRUSTER_BLOCK_ENTITY.get()) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }
}
