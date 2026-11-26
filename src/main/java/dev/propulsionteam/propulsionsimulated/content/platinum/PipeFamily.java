package dev.propulsionteam.propulsionsimulated.content.platinum;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Platinum pipes and Create's copper pipes never connect to each other.
 * Every other fluid component (pumps, valves, smart pipes, tanks) accepts both.
 */
public enum PipeFamily {
    COPPER, PLATINUM;

    private static final Set<String> COPPER_PIPES = Set.of("fluid_pipe", "glass_fluid_pipe", "encased_fluid_pipe");

    private static PipeFamily of(BlockState state) {
        Block block = state.getBlock();
        // Checked first: platinum pipes subclass the Create pipe classes
        if (block instanceof PlatinumPipeBlock || block instanceof GlassPlatinumPipeBlock) {
            return PLATINUM;
        }
        // Matched by id: Create's block constants expose Registrate types we don't compile against
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id.getNamespace().equals("create") && COPPER_PIPES.contains(id.getPath())) {
            return COPPER;
        }
        return null;
    }

    public static boolean conflict(BlockState first, BlockState second) {
        PipeFamily a = of(first);
        PipeFamily b = of(second);
        return a != null && b != null && a != b;
    }

    public static boolean conflictsWithNeighbour(Level level, BlockPos pos, BlockState state, Direction side) {
        if (level == null) {
            return false;
        }
        BlockPos neighbour = pos.relative(side);
        return level.isLoaded(neighbour) && conflict(state, level.getBlockState(neighbour));
    }
}
