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

import java.util.IdentityHashMap;
import java.util.Map;
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

    private static Vector3d worldPosition(@Nullable SubLevel subLevel, BlockPos pos) {
        Vector3d center = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return subLevel != null ? subLevel.logicalPose().transformPosition(center) : center;
    }

    private static boolean isScoped(IRedstoneLinkable link) {
        return link instanceof ShipScoped scoped && scoped.isShipScoped();
    }

    public static int powerAt(LevelAccessor world, Iterable<IRedstoneLinkable> network, IRedstoneLinkable receiver,
                              Predicate<IRedstoneLinkable> ignore) {
        return powerAt(world, network, receiver, ignore, Map.of());
    }

    private static int powerAt(LevelAccessor world, Iterable<IRedstoneLinkable> network, IRedstoneLinkable receiver,
                               Predicate<IRedstoneLinkable> ignore, Map<IRedstoneLinkable, SubLevel> scopedShips) {
        SubLevel receiverShip = subLevelOf(world, receiver.getLocation());
        Vector3d receiverPos = worldPosition(receiverShip, receiver.getLocation());
        double range = linkRange();
        double rangeSquared = range * range;
        int power = 0;
        for (IRedstoneLinkable link : network) {
            if (link == receiver || ignore.test(link) || !link.isAlive()) continue;
            int strength = Math.min(15, link.getTransmittedStrength());
            if (strength <= power) continue;
            SubLevel ship;
            if (scopedShips.containsKey(link)) {
                ship = scopedShips.get(link);
                if (ship == null || ship != receiverShip) continue;
            } else {
                ship = subLevelOf(world, link.getLocation());
                if (isScoped(link) && (ship == null || ship != receiverShip)) continue;
            }
            if (worldPosition(ship, link.getLocation()).distanceSquared(receiverPos) < rangeSquared) power = strength;
        }
        return power;
    }

    public static int adjustReceived(RedstoneLinkNetworkHandler handler, LevelAccessor world,
                                     IRedstoneLinkable receiver, int power) {
        Set<IRedstoneLinkable> network = handler.networksIn(world).get(receiver.getNetworkKey());
        if (network == null) return power;
        Map<IRedstoneLinkable, SubLevel> scopedShips = null;
        for (IRedstoneLinkable link : network) {
            if (isScoped(link) && link.isAlive() && link.getTransmittedStrength() > 0) {
                if (scopedShips == null) scopedShips = new IdentityHashMap<>();
                scopedShips.put(link, subLevelOf(world, link.getLocation()));
            }
        }
        if (scopedShips == null) return power;
        return powerAt(world, network, receiver, link -> false, scopedShips);
    }
}
