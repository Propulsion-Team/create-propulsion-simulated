package dev.propulsionteam.propulsionsimulated.content.wing;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;


public class SymWingPlacementHelper implements IPlacementHelper {
    private List<Supplier<? extends net.minecraft.world.level.block.Block>> blockEntries;

    public SymWingPlacementHelper(List<Supplier<? extends net.minecraft.world.level.block.Block>> blockEntries) {
        this.blockEntries = blockEntries;
    }

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return (stack) -> blockEntries.stream().anyMatch(be -> stack.is(be.get().asItem()));
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return (state) -> blockEntries.stream().anyMatch(be -> state.is(be.get()));
    }

    @Override
    public PlacementOffset getOffset(@Nonnull Player player, @Nonnull Level world, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nonnull BlockHitResult ray) {
        Vec3 result = ray.getLocation();

        List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, result,
            state.getValue(BlockStateProperties.FACING).getAxis(),
            dir -> world.getBlockState(pos.relative(dir)).canBeReplaced());

        if (directions.isEmpty()) {
            return PlacementOffset.fail();
        }

        return PlacementOffset.success(pos.relative(directions.get(0)),
            s -> s.setValue(BlockStateProperties.FACING, state.getValue(BlockStateProperties.FACING)));
    }
}
