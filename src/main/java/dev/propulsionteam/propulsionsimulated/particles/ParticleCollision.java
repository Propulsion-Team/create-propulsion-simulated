package dev.propulsionteam.propulsionsimulated.particles;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Intersectiond;
import org.joml.Vector2d;
import org.joml.Vector3d;

public final class ParticleCollision {
    private ParticleCollision() {}

    public static Hit sweep(Level level, AABB bounds, Vec3 movement, SubLevel ignored) {
        Vec3 center = bounds.getCenter();
        Vector3d start = new Vector3d(center.x, center.y, center.z);
        Vector3d delta = new Vector3d(movement.x, movement.y, movement.z);
        double radius = Math.max(bounds.getXsize(), Math.max(bounds.getYsize(), bounds.getZsize())) * 0.5d;
        AABB swept = bounds.expandTowards(movement);
        ParticleCollisionMath.Hit worldHit = scan(level, start, delta, radius, swept,
                level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        Hit best = worldHit == null ? null : new Hit(worldHit.fraction(), movement.scale(worldHit.fraction()),
                new Vec3(worldHit.normal().x, worldHit.normal().y, worldHit.normal().z));
        for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(swept))) {
            if (subLevel == ignored || subLevel.isRemoved()) continue;
            var pose = subLevel.logicalPose();
            Vector3d localStart = subLevel.lastPose().transformPositionInverse(new Vector3d(start));
            Vector3d localEnd = pose.transformPositionInverse(new Vector3d(start).add(delta));
            Vector3d localDelta = new Vector3d(localEnd).sub(localStart);
            double localRadius = radius / ParticleScale.valid(pose.scale().x());
            AABB localBounds = new AABB(localStart.x, localStart.y, localStart.z,
                    localEnd.x, localEnd.y, localEnd.z).inflate(localRadius);
            var plot = subLevel.getPlot().getBoundingBox();
            AABB plotBounds = new AABB(plot.minX(), plot.minY(), plot.minZ(),
                    plot.maxX() + 1.0d, plot.maxY() + 1.0d, plot.maxZ() + 1.0d);
            if (!localBounds.intersects(plotBounds)) continue;
            localBounds = localBounds.intersect(plotBounds);
            ParticleCollisionMath.Hit hit = scan(level, localStart, localDelta, localRadius,
                    localBounds, plot.minY(), plot.maxY());
            if (hit != null && (best == null || hit.fraction() < best.fraction())) {
                Vector3d normal = pose.transformNormal(new Vector3d(hit.normal())).normalize();
                Vector3d position = pose.transformPosition(new Vector3d(localStart).fma(hit.fraction(), localDelta));
                best = new Hit(hit.fraction(), new Vec3(position.x - start.x, position.y - start.y, position.z - start.z),
                        new Vec3(normal.x, normal.y, normal.z));
            }
        }
        return best;
    }

    private static ParticleCollisionMath.Hit scan(Level level, Vector3d start, Vector3d movement,
                                                 double radius, AABB bounds, int minY, int maxY) {
        int y0 = Math.max(minY, (int) Math.floor(bounds.minY));
        int y1 = Math.min(maxY, (int) Math.floor(bounds.maxY));
        if (y0 > y1) return null;
        if (movement.lengthSquared() < 1.0e-20d) return null;
        Vector2d interval = new Vector2d();
        AABB rayBounds = bounds.inflate(radius);
        if (!Intersectiond.intersectRayAab(start.x, start.y, start.z, movement.x, movement.y, movement.z,
                rayBounds.minX, rayBounds.minY, rayBounds.minZ, rayBounds.maxX, rayBounds.maxY, rayBounds.maxZ, interval)) return null;
        Vector3d from = new Vector3d(start).fma(Math.max(0.0d, interval.x), movement);
        Vector3d to = new Vector3d(start).fma(Math.min(1.0d, interval.y), movement);
        ParticleCollisionMath.Hit[] best = {null};
        int padding = (int) Math.ceil(radius);
        BlockGetter.traverseBlocks(new Vec3(from.x, from.y, from.z), new Vec3(to.x, to.y, to.z), best, (result, center) -> {
            int x0 = Math.max((int) Math.floor(bounds.minX), center.getX() - padding);
            int x1 = Math.min((int) Math.floor(bounds.maxX), center.getX() + padding);
            int z0 = Math.max((int) Math.floor(bounds.minZ), center.getZ() - padding);
            int z1 = Math.min((int) Math.floor(bounds.maxZ), center.getZ() + padding);
            int lowY = Math.max(y0, center.getY() - padding);
            int highY = Math.min(y1, center.getY() + padding);
            if (x0 > x1 || lowY > highY || z0 > z1) return null;
            for (BlockPos pos : BlockPos.betweenClosed(x0, lowY, z0, x1, highY, z1)) {
                if (!level.hasChunkAt(pos)) continue;
                var state = level.getBlockState(pos);
                if (state.isAir()) continue;
                for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
                    ParticleCollisionMath.Hit hit = ParticleCollisionMath.sweep(start, movement,
                            box.minX + pos.getX(), box.minY + pos.getY(), box.minZ + pos.getZ(),
                            box.maxX + pos.getX(), box.maxY + pos.getY(), box.maxZ + pos.getZ(), radius);
                    if (hit != null && (result[0] == null || hit.fraction() < result[0].fraction())) result[0] = hit;
                }
            }
            return null;
        }, result -> null);
        return best[0];
    }

    public record Hit(double fraction, Vec3 movement, Vec3 normal) {}
}
