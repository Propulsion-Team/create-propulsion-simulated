package dev.propulsionteam.propulsionsimulated.utility.redstone_link;

import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Set;
import java.util.function.Predicate;

public final class ShipScopedLinks {
    public interface ShipScoped extends IRedstoneLinkable {
        boolean isShipScoped();
    }

    private ShipScopedLinks() {}

    public static int linkRange() {
        return AllConfigs.server().logistics.linkRange.get();
    }

    @Nullable
    public static SubLevel subLevelOf(LevelAccessor world, BlockPos pos) {
        return world instanceof Level level ? Sable.HELPER.getContaining(level, pos) : null;
    }

    private static Vector3d worldPosition(LevelAccessor world, BlockPos pos) {
        Vector3d center = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        SubLevel subLevel = subLevelOf(world, pos);
        return subLevel != null ? subLevel.logicalPose().transformPosition(center) : center;
    }

    private static boolean isScoped(IRedstoneLinkable link) {
        return link instanceof ShipScoped scoped && scoped.isShipScoped();
    }

    private static boolean reaches(LevelAccessor world, IRedstoneLinkable link, IRedstoneLinkable receiver) {
        if (isScoped(link)) {
            SubLevel ship = subLevelOf(world, link.getLocation());
            if (ship == null || ship != subLevelOf(world, receiver.getLocation())) return false;
        }
        double range = linkRange();
        return worldPosition(world, link.getLocation())
            .distanceSquared(worldPosition(world, receiver.getLocation())) < range * range;
    }

    public static int powerAt(LevelAccessor world, Iterable<IRedstoneLinkable> network, IRedstoneLinkable receiver,
                              Predicate<IRedstoneLinkable> ignore) {
        int power = 0;
        for (IRedstoneLinkable link : network) {
            if (link == receiver || ignore.test(link) || !link.isAlive()) continue;
            int strength = Math.min(15, link.getTransmittedStrength());
            if (strength > power && reaches(world, link, receiver)) power = strength;
        }
        return power;
    }

    public static int adjustReceived(RedstoneLinkNetworkHandler handler, LevelAccessor world,
                                     IRedstoneLinkable receiver, int power) {
        Set<IRedstoneLinkable> network = handler.networksIn(world).get(receiver.getNetworkKey());
        if (network == null || network.stream().noneMatch(ShipScopedLinks::isScoped)) return power;
        return powerAt(world, network, receiver, link -> false);
    }
}
