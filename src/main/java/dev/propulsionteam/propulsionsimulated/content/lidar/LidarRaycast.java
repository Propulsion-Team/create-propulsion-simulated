package dev.propulsionteam.propulsionsimulated.content.lidar;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class LidarRaycast {
    public static final double MAX_RANGE = 64.0;
    public static final double MAX_ANGLE_DEGREES = 90.0;
    public static final int MAX_SCAN_RAYS = 4096;
    private static final double SENSOR_FACE_OFFSET = 6.0 / 16.0 - 0.5 + 0.01;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private LidarRaycast() {}

    public record Options(double range, @Nullable Vector3dc direction, boolean fluids, boolean nonSolid,
                          boolean seeThrough, boolean entities, boolean players, boolean subLevels, boolean ownShip,
                          @Nullable Predicate<BlockState> include, @Nullable Predicate<BlockState> exclude,
                          boolean blocks, boolean precise) {
        public static Options defaults() {
            return new Options(MAX_RANGE, null, false, false, true, true, true, true, false, null, null, true, true);
        }

        boolean anyEntities() {
            return entities || players;
        }
    }

    public record Hit(double distance, Vec3 position, @Nullable BlockPos blockPos, @Nullable BlockState state,
                      @Nullable Entity entity, @Nullable SubLevel subLevel, boolean ship) {
        Object key() {
            return ship ? subLevel : entity != null ? entity : blockPos;
        }

        Hit asShip() {
            return new Hit(distance, position, blockPos, state, entity, subLevel, true);
        }
    }

    public static Vector3d normal(Direction facing) {
        return new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
    }

    public static boolean withinCone(Direction facing, Vector3dc direction, double spreadRadians) {
        return normal(facing).angle(direction) + spreadRadians <= Math.toRadians(MAX_ANGLE_DEGREES) + 1.0e-9;
    }

    @Nullable
    public static Hit cast(Level level, BlockPos pos, Direction facing, Options options) {
        Scanner scanner = new Scanner(level, pos, facing, options);
        Vector3d direction = scanner.toWorld(scanner.center());
        Hit best = options.blocks() ? scanner.blocks(direction, options.range()) : null;
        if (options.anyEntities()) {
            Vec3 start = scanner.startVec();
            Vec3 end = start.add(direction.x * options.range(), direction.y * options.range(), direction.z * options.range());
            double limit = best != null ? best.distance() : options.range();
            for (Entity entity : scanner.entities(new AABB(start, end).inflate(1.0))) {
                Optional<Vec3> point = entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start, end);
                if (point.isEmpty()) continue;
                double distance = start.distanceTo(point.get());
                if (distance < limit && (options.blocks() || scanner.blocks(dirTo(start, point.get()), distance) == null)) {
                    limit = distance;
                    best = new Hit(distance, point.get(), null, null, entity, null, false);
                }
            }
        }
        return best;
    }

    private static Vector3d dirTo(Vec3 from, Vec3 to) {
        return new Vector3d(to.x - from.x, to.y - from.y, to.z - from.z).normalize();
    }

    public static List<Hit> scan(Level level, BlockPos pos, Direction facing, Options options,
                                 double spreadRadians, int rays) {
        Scanner scanner = new Scanner(level, pos, facing, options);
        Vector3d center = scanner.center();
        Vector3d helper = Math.abs(center.y) < 0.9 ? new Vector3d(0, 1, 0) : new Vector3d(1, 0, 0);
        Vector3d side1 = center.cross(helper, new Vector3d()).normalize();
        Vector3d side2 = center.cross(side1, new Vector3d()).normalize();

        Map<Object, Hit> nearest = new HashMap<>();
        if (options.blocks()) {
            double capHeight = 1.0 - Math.cos(spreadRadians);
            for (int i = 0; i < rays; i++) {
                double cos = 1.0 - (i + 0.5) / rays * capHeight;
                double sin = Math.sqrt(Math.max(0.0, 1.0 - cos * cos));
                double azimuth = i * GOLDEN_ANGLE;
                Vector3d local = new Vector3d(center).mul(cos)
                    .fma(sin * Math.cos(azimuth), side1)
                    .fma(sin * Math.sin(azimuth), side2);
                keepNearest(nearest, scanner.blocks(scanner.toWorld(local), options.range()));
            }
        }

        Vector3d axis = scanner.toWorld(center);
        double cosSpread = Math.cos(spreadRadians);
        Vec3 start = scanner.startVec();
        if (options.anyEntities()) {
            for (Entity entity : scanner.entities(new AABB(start, start).inflate(options.range()))) {
                AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                Vec3 middle = box.getCenter();
                for (Vec3 sample : List.of(middle, new Vec3(middle.x, box.maxY - 0.1, middle.z),
                    new Vec3(middle.x, box.minY + 0.1, middle.z))) {
                    Vector3d direction = aimAt(start, sample, axis, cosSpread, options.range());
                    if (direction == null) continue;
                    Vec3 point = box.contains(start) ? start : box.clip(start, sample).orElse(sample);
                    double distance = start.distanceTo(point);
                    if (scanner.blocks(direction, distance) == null) {
                        keepNearest(nearest, new Hit(distance, point, null, null, entity, null, false));
                        break;
                    }
                }
            }
        }

        if (!options.blocks() && options.subLevels()) {
            for (SubLevel ship : scanner.ships) {
                if (ship == scanner.own) continue;
                BoundingBox3dc box = ship.boundingBox();
                double reach = Math.sqrt(Math.pow(box.maxX() - box.minX(), 2) + Math.pow(box.maxY() - box.minY(), 2)
                    + Math.pow(box.maxZ() - box.minZ(), 2));
                for (Vec3 sample : shipSamples(box)) {
                    Vector3d direction = aimAt(start, sample, axis, cosSpread, options.range());
                    if (direction == null) continue;
                    Hit hit = scanner.blocks(direction, Math.min(options.range(), start.distanceTo(sample) + reach));
                    if (hit != null && hit.subLevel() == ship) {
                        keepNearest(nearest, hit.asShip());
                        break;
                    }
                }
            }
        }

        List<Hit> hits = new ArrayList<>(nearest.values());
        hits.sort(Comparator.comparingDouble(Hit::distance));
        return hits;
    }

    @Nullable
    private static Vector3d aimAt(Vec3 start, Vec3 target, Vector3d axis, double cosSpread, double range) {
        Vector3d direction = new Vector3d(target.x - start.x, target.y - start.y, target.z - start.z);
        double distance = direction.length();
        if (distance < 1.0e-6 || distance > range) return null;
        direction.div(distance);
        return direction.dot(axis) >= cosSpread ? direction : null;
    }

    private static List<Vec3> shipSamples(BoundingBox3dc box) {
        double cx = (box.minX() + box.maxX()) / 2, cy = (box.minY() + box.maxY()) / 2, cz = (box.minZ() + box.maxZ()) / 2;
        double hx = (box.maxX() - box.minX()) / 4, hy = (box.maxY() - box.minY()) / 4, hz = (box.maxZ() - box.minZ()) / 4;
        List<Vec3> samples = new ArrayList<>(9);
        samples.add(new Vec3(cx, cy, cz));
        for (int i = 0; i < 8; i++) {
            samples.add(new Vec3(cx + ((i & 1) == 0 ? -hx : hx), cy + ((i & 2) == 0 ? -hy : hy), cz + ((i & 4) == 0 ? -hz : hz)));
        }
        return samples;
    }

    private static void keepNearest(Map<Object, Hit> nearest, @Nullable Hit hit) {
        if (hit != null) nearest.merge(hit.key(), hit, (a, b) -> a.distance() <= b.distance() ? a : b);
    }

    private static final class Scanner {
        final Level level;
        final BlockPos self;
        final Options options;
        final Vector3d normal;
        @Nullable
        final SubLevel own;
        final Vector3d start;
        final List<SubLevel> ships = new ArrayList<>();
        final Map<BlockState, VoxelShape> shapes = new IdentityHashMap<>();
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        final int minY;
        final int maxY;

        Scanner(Level level, BlockPos pos, Direction facing, Options options) {
            this.level = level;
            this.self = pos;
            this.options = options;
            this.normal = normal(facing);
            this.own = Sable.HELPER.getContaining(level, pos);
            Vector3d origin = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).fma(SENSOR_FACE_OFFSET, normal);
            this.start = own != null ? own.logicalPose().transformPosition(origin, new Vector3d()) : origin;
            this.minY = level.getMinBuildHeight();
            this.maxY = level.getMaxBuildHeight();

            double r = options.range();
            BoundingBox3d area = new BoundingBox3d(start.x - r, start.y - r, start.z - r, start.x + r, start.y + r, start.z + r);
            for (SubLevel ship : Sable.HELPER.getAllIntersecting(level, area)) {
                if (!ship.isRemoved() && (ship == own ? options.ownShip() : options.subLevels())) ships.add(ship);
            }
        }

        Vector3d center() {
            return options.direction() != null ? new Vector3d(options.direction()).normalize() : new Vector3d(normal);
        }

        Vector3d toWorld(Vector3dc local) {
            Vector3d world = own != null ? own.logicalPose().transformNormal(local, new Vector3d()) : new Vector3d(local);
            return world.normalize();
        }

        Vec3 startVec() {
            return new Vec3(start.x, start.y, start.z);
        }

        List<Entity> entities(AABB area) {
            return level.getEntities((Entity) null, area, e -> !e.isSpectator() && e.isPickable()
                && (e instanceof Player ? options.players() : options.entities()));
        }

        @Nullable
        Hit blocks(Vector3d direction, double range) {
            Hit best = walk(null, start, direction, 1.0, 0.0, range);
            for (SubLevel ship : ships) {
                double limit = best != null ? best.distance() : range;
                double[] span = slab(ship.boundingBox(), start, direction, limit);
                if (span == null) continue;
                Pose3dc pose = ship.logicalPose();
                Vector3d entry = new Vector3d(direction).mul(span[0]).add(start);
                Vector3d origin = pose.transformPositionInverse(entry, new Vector3d());
                Vector3d localDirection = pose.transformNormalInverse(direction, new Vector3d());
                double scale = localDirection.length();
                if (scale < 1.0e-12) continue;
                localDirection.div(scale);
                Hit hit = walk(ship, origin, localDirection, scale, span[0], (span[1] - span[0]) * scale);
                if (hit != null && (best == null || hit.distance() < best.distance())) best = hit;
            }
            return best;
        }

        @Nullable
        private Hit walk(@Nullable SubLevel space, Vector3d o, Vector3d d, double scale, double offset, double tMax) {
            int x = (int) Math.floor(o.x), y = (int) Math.floor(o.y), z = (int) Math.floor(o.z);
            int sx = (int) Math.signum(d.x), sy = (int) Math.signum(d.y), sz = (int) Math.signum(d.z);
            double dx = sx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / d.x);
            double dy = sy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / d.y);
            double dz = sz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / d.z);
            double nx = sx == 0 ? Double.POSITIVE_INFINITY : ((sx > 0 ? x + 1 : x) - o.x) / d.x;
            double ny = sy == 0 ? Double.POSITIVE_INFINITY : ((sy > 0 ? y + 1 : y) - o.y) / d.y;
            double nz = sz == 0 ? Double.POSITIVE_INFINITY : ((sz > 0 ? z + 1 : z) - o.z) / d.z;
            boolean ownSpace = space == own;

            LevelChunk chunk = null;
            int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;
            double t = 0.0;
            while (t <= tMax) {
                if (y < minY || y >= maxY) {
                    if (y < minY ? sy <= 0 : sy >= 0) return null;
                } else {
                    if ((x >> 4) != chunkX || (z >> 4) != chunkZ) {
                        chunkX = x >> 4;
                        chunkZ = z >> 4;
                        chunk = chunk(space, chunkX, chunkZ);
                        if (chunk == null) return null;
                    }
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                    if (!section.hasOnlyAir()) {
                        BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                        if (!state.isAir() && !(ownSpace && x == self.getX() && y == self.getY() && z == self.getZ())) {
                            VoxelShape shape = shape(state);
                            if (!shape.isEmpty()) {
                                cursor.set(x, y, z);
                                Vec3 from = new Vec3(o.x + d.x * t, o.y + d.y * t, o.z + d.z * t);
                                if (!options.precise()) return hit(space, o, scale, offset, from, state);
                                Vec3 to = new Vec3(o.x + d.x * tMax, o.y + d.y * tMax, o.z + d.z * tMax);
                                BlockHitResult result = shape.clip(from, to, cursor);
                                if (result != null) return hit(space, o, scale, offset, result.getLocation(), state);
                            }
                        }
                    }
                }
                if (nx < ny && nx < nz) {
                    x += sx;
                    t = nx;
                    nx += dx;
                } else if (ny < nz) {
                    y += sy;
                    t = ny;
                    ny += dy;
                } else {
                    z += sz;
                    t = nz;
                    nz += dz;
                }
            }
            return null;
        }

        private Hit hit(@Nullable SubLevel space, Vector3d o, double scale, double offset, Vec3 local, BlockState state) {
            double distance = offset + local.distanceTo(new Vec3(o.x, o.y, o.z)) / scale;
            Vec3 position = local;
            if (space != null) {
                Vector3d world = space.logicalPose().transformPosition(new Vector3d(local.x, local.y, local.z), new Vector3d());
                position = new Vec3(world.x, world.y, world.z);
            }
            return new Hit(distance, position, cursor.immutable(), state, null, space, false);
        }

        @Nullable
        private LevelChunk chunk(@Nullable SubLevel space, int chunkX, int chunkZ) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            return chunk == null && space != null ? level.getChunk(chunkX, chunkZ) : chunk;
        }

        private VoxelShape shape(BlockState state) {
            return shapes.computeIfAbsent(state, this::computeShape);
        }

        private VoxelShape computeShape(BlockState state) {
            if (!options.seeThrough() && !state.canOcclude()) return Shapes.empty();
            if (options.exclude() != null && options.exclude().test(state)) return Shapes.empty();
            if (options.include() != null && !options.include().test(state)) return Shapes.empty();
            FluidState fluid = state.getFluidState();
            VoxelShape fluidShape = options.fluids() && !fluid.isEmpty()
                ? Block.box(0, 0, 0, 16, Math.max(1, fluid.getOwnHeight() * 16), 16) : Shapes.empty();
            if (state.getBlock() instanceof LiquidBlock) return fluidShape;
            VoxelShape shape = options.nonSolid()
                ? state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                : state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            return fluidShape.isEmpty() ? shape : Shapes.or(shape, fluidShape);
        }

        @Nullable
        private static double[] slab(BoundingBox3dc box, Vector3d o, Vector3d d, double tMax) {
            double t0 = 0.0, t1 = tMax;
            double[] min = {box.minX(), box.minY(), box.minZ()};
            double[] max = {box.maxX(), box.maxY(), box.maxZ()};
            double[] origin = {o.x, o.y, o.z};
            double[] dir = {d.x, d.y, d.z};
            for (int i = 0; i < 3; i++) {
                if (Math.abs(dir[i]) < 1.0e-12) {
                    if (origin[i] < min[i] || origin[i] > max[i]) return null;
                    continue;
                }
                double a = (min[i] - origin[i]) / dir[i];
                double b = (max[i] - origin[i]) / dir[i];
                t0 = Math.max(t0, Math.min(a, b));
                t1 = Math.min(t1, Math.max(a, b));
                if (t0 > t1) return null;
            }
            return new double[] {t0, t1};
        }
    }
}
