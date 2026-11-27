package dev.propulsionteam.propulsionsimulated.content.lidar;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.CCProxy;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LidarBlockEntity extends SmartBlockEntity {
    public AbstractComputerBehaviour computerBehaviour;

    public LidarBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.LIDAR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = CCProxy.behaviour(this));
        }
    }

    public Direction getFacing() {
        return getBlockState().getValue(LidarBlock.FACING);
    }

    @Nullable
    public LidarRaycast.Hit raycast(LidarRaycast.Options options) {
        if (level == null) return null;
        LidarDebug.Trace trace = LidarDebug.start(level);
        LidarRaycast.Hit hit = LidarRaycast.cast(level, worldPosition, getFacing(), options, trace);
        LidarDebug.send(level, worldPosition, trace);
        return hit;
    }

    public CompletableFuture<List<LidarRaycast.Hit>> scan(LidarRaycast.Options options, double spreadRadians, int rays) {
        if (level == null) return CompletableFuture.completedFuture(List.of());
        Level level = this.level;
        BlockPos pos = worldPosition;
        LidarDebug.Trace trace = LidarDebug.start(level);
        return LidarRaycast.scan(level, pos, getFacing(), options, spreadRadians, rays, trace)
            .whenComplete((hits, error) -> LidarDebug.send(level, pos, trace));
    }
}
