package dev.propulsionteam.propulsionsimulated.content.lidar;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class LidarRaycast {
    public static final double MAX_RANGE = 64.0;
    public static final double MAX_ANGLE_DEGREES = 90.0;
    public static final int MAX_SCAN_RAYS = 4096;
    private static final double SENSOR_FACE_OFFSET = 6.0 / 16.0 - 0.5 + 0.01;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final int RAYS_PER_TASK = 256;
    private static final int MAX_PLOT_CHUNKS = 1024;
    private static final int WORKERS = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
    private static final ExecutorService POOL = Executors.newFixedThreadPool(WORKERS, new java.util.concurrent.ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "Lidar Worker " + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    });

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
                      @Nullable EntityInfo entity, @Nullable SubLevel subLevel, boolean ship) {
        Object key() {
            return ship ? subLevel : entity != null ? entity.id() : blockPos;
        }

        Hit asShip() {
            return new Hit(distance, position, blockPos, state, entity, subLevel, true);
        }
    }

    public record EntityInfo(UUID id, String kind, String type, Vec3 position, @Nullable String name) {}

    public static Vector3d normal(Direction facing) {
        return new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
    }

    public static boolean withinCone(Direction facing, Vector3dc direction, double spreadRadians) {
        return normal(facing).angle(direction) + spreadRadians <= Math.toRadians(MAX_ANGLE_DEGREES) + 1.0e-9;
    }

    @Nullable
    public static Hit cast(Level level, BlockPos pos, Direction facing, Options options, @Nullable LidarDebug.Trace trace) {
        Scene scene = new Scene(level, pos, facing, options, trace, false);
        Vector3d direction = scene.toWorld(scene.center());
        Hit wall = scene.blocks(direction, options.range());
        Hit best = scene.accept(wall);
        if (options.anyEntities()) {
            Vec3 start = scene.startVec();
            Vec3 end = start.add(direction.x * options.range(), direction.y * options.range(), direction.z * options.range());
            double limit = wall != null ? wall.distance() : options.range();
            for (Target target : targets(level, new AABB(start, end).inflate(1.0), options)) {
                Optional<Vec3> point = target.box().clip(start, end);
                if (point.isEmpty()) continue;
                double distance = start.distanceTo(point.get());
                if (distance < limit) {
                    limit = distance;
                    best = new Hit(distance, point.get(), null, null, target.info(), null, false);
                }
            }
            if (best != null && best.entity() != null) scene.log(best.position(), LidarDebug.ENTITY);
        }
        return best;
    }

    public static CompletableFuture<List<Hit>> scan(Level level, BlockPos pos, Direction facing, Options options,
                                                    double spreadRadians, int rays, @Nullable LidarDebug.Trace trace) {
        Scene scene = new Scene(level, pos, facing, options, trace, true);
        List<CompletableFuture<Map<Object, Hit>>> parts = new ArrayList<>();
        if (options.blocks()) {
            for (int from = 0; from < rays; from += RAYS_PER_TASK) {
                int first = from, last = Math.min(rays, from + RAYS_PER_TASK);
                parts.add(CompletableFuture.supplyAsync(() -> scene.sample(spreadRadians, rays, first, last), POOL));
            }
        }
        parts.add(CompletableFuture.supplyAsync(() -> scene.targets(spreadRadians), POOL));
        return CompletableFuture.allOf(parts.toArray(CompletableFuture[]::new)).thenApply(done -> {
            Map<Object, Hit> nearest = new HashMap<>();
            for (CompletableFuture<Map<Object, Hit>> part : parts) {
                for (Hit hit : part.join().values()) keepNearest(nearest, hit);
            }
            List<Hit> hits = new ArrayList<>(nearest.values());
            hits.sort(Comparator.comparingDouble(Hit::distance));
            return hits;
        });
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

    private record Target(EntityInfo info, AABB box) {}

    private static List<Target> targets(Level level, AABB area, Options options) {
        List<Target> targets = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, area, e -> !e.isSpectator() && e.isPickable()
            && (e instanceof Player ? options.players() : options.entities()))) {
            String name = entity instanceof Player player ? player.getGameProfile().getName()
                : entity.hasCustomName() ? entity.getCustomName().getString() : null;
            String kind = entity instanceof Player ? "player" : entity instanceof LivingEntity ? "mob" : "entity";
            EntityInfo info = new EntityInfo(entity.getUUID(), kind, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
                entity.position(), name);
            targets.add(new Target(info, entity.getBoundingBox().inflate(entity.getPickRadius())));
        }
        return targets;
    }

    private record Ship(SubLevel subLevel, Pose3dc pose, BoundingBox3dc box, Long2ObjectOpenHashMap<LevelChunk> chunks) {}

    private static final class Scene {
        final BlockPos self;
        final Options options;
        final Vector3d normal;
        @Nullable
        final Pose3dc ownPose;
        @Nullable
        final Ship own;
        final Vector3d start;
        final Long2ObjectOpenHashMap<LevelChunk> worldChunks = new Long2ObjectOpenHashMap<>();
        final List<Ship> ships = new ArrayList<>();
        final List<Target> entities;
        final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();
        final int minY;
        final int maxY;
        @Nullable
        final LidarDebug.Trace trace;

        Scene(Level level, BlockPos pos, Direction facing, Options options, @Nullable LidarDebug.Trace trace, boolean collectEntities) {
            this.self = pos;
            this.options = options;
            this.trace = trace;
            this.normal = normal(facing);
            this.minY = level.getMinBuildHeight();
            this.maxY = level.getMaxBuildHeight();

            SubLevel ownLevel = Sable.HELPER.getContaining(level, pos);
            this.ownPose = ownLevel != null ? new Pose3d(ownLevel.logicalPose()) : null;
            Vector3d origin = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).fma(SENSOR_FACE_OFFSET, normal);
            this.start = ownPose != null ? ownPose.transformPosition(origin, new Vector3d()) : origin;

            double r = options.range();
            BoundingBox3d area = new BoundingBox3d(start.x - r, start.y - r, start.z - r, start.x + r, start.y + r, start.z + r);
            addChunks(level, worldChunks, area, false, 0);
            int budget = MAX_PLOT_CHUNKS;

            Ship ownShip = null;
            for (SubLevel ship : Sable.HELPER.getAllIntersecting(level, area)) {
                boolean mine = ship == ownLevel;
                if (ship.isRemoved() || mine && !options.ownShip()) continue;
                Pose3dc pose = mine ? ownPose : new Pose3d(ship.logicalPose());
                BoundingBox3d box = new BoundingBox3d(ship.boundingBox());
                Long2ObjectOpenHashMap<LevelChunk> chunks = new Long2ObjectOpenHashMap<>();
                BoundingBox3d near = new BoundingBox3d(Math.max(box.minX(), area.minX()), Math.max(box.minY(), area.minY()),
                    Math.max(box.minZ(), area.minZ()), Math.min(box.maxX(), area.maxX()), Math.min(box.maxY(), area.maxY()),
                    Math.min(box.maxZ(), area.maxZ()));
                if (near.minX() <= near.maxX() && near.minY() <= near.maxY() && near.minZ() <= near.maxZ() && budget > 0) {
                    budget -= addChunks(level, chunks, near.transformInverse(pose), true, budget);
                }
                Ship snapshot = new Ship(ship, pose, box, chunks);
                ships.add(snapshot);
                if (mine) ownShip = snapshot;
            }
            this.own = ownShip;

            this.entities = collectEntities && options.anyEntities()
                ? LidarRaycast.targets(level, new AABB(start.x - r, start.y - r, start.z - r, start.x + r, start.y + r, start.z + r), options)
                : List.of();
        }

        private static int addChunks(Level level, Long2ObjectOpenHashMap<LevelChunk> chunks, BoundingBox3dc box, boolean load, int limit) {
            int added = 0;
            int minX = (int) Math.floor(box.minX()) >> 4, maxX = (int) Math.floor(box.maxX()) >> 4;
            int minZ = (int) Math.floor(box.minZ()) >> 4, maxZ = (int) Math.floor(box.maxZ()) >> 4;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (load && added >= limit) return added;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                    if (chunk == null && load) chunk = level.getChunk(x, z);
                    if (chunk != null) {
                        chunks.put(ChunkPos.asLong(x, z), chunk);
                        added++;
                    }
                }
            }
            return added;
        }

        Vector3d center() {
            return options.direction() != null ? new Vector3d(options.direction()).normalize() : new Vector3d(normal);
        }

        Vector3d toWorld(Vector3dc local) {
            Vector3d world = ownPose != null ? ownPose.transformNormal(local, new Vector3d()) : new Vector3d(local);
            return world.normalize();
        }

        Vec3 startVec() {
            return new Vec3(start.x, start.y, start.z);
        }

        Map<Object, Hit> sample(double spreadRadians, int rays, int first, int last) {
            Vector3d center = center();
            Vector3d helper = Math.abs(center.y) < 0.9 ? new Vector3d(0, 1, 0) : new Vector3d(1, 0, 0);
            Vector3d side1 = center.cross(helper, new Vector3d()).normalize();
            Vector3d side2 = center.cross(side1, new Vector3d()).normalize();
            double capHeight = 1.0 - Math.cos(spreadRadians);
            Map<Object, Hit> nearest = new HashMap<>();
            for (int i = first; i < last; i++) {
                double cos = 1.0 - (i + 0.5) / rays * capHeight;
                double sin = Math.sqrt(Math.max(0.0, 1.0 - cos * cos));
                double azimuth = i * GOLDEN_ANGLE;
                Vector3d local = new Vector3d(center).mul(cos)
                    .fma(sin * Math.cos(azimuth), side1)
                    .fma(sin * Math.sin(azimuth), side2);
                keepNearest(nearest, accept(blocks(toWorld(local), options.range())));
            }
            return nearest;
        }

        Map<Object, Hit> targets(double spreadRadians) {
            Map<Object, Hit> nearest = new HashMap<>();
            Vector3d axis = toWorld(center());
            double cosSpread = Math.cos(spreadRadians);
            Vec3 start = startVec();
            for (Target target : entities) {
                AABB box = target.box();
                Vec3 middle = box.getCenter();
                for (Vec3 sample : List.of(middle, new Vec3(middle.x, box.maxY - 0.1, middle.z),
                    new Vec3(middle.x, box.minY + 0.1, middle.z))) {
                    Vector3d direction = aimAt(start, sample, axis, cosSpread, options.range());
                    if (direction == null) continue;
                    Vec3 point = box.contains(start) ? start : box.clip(start, sample).orElse(sample);
                    double distance = start.distanceTo(point);
                    if (blocks(direction, distance) == null) {
                        log(point, LidarDebug.ENTITY);
                        keepNearest(nearest, new Hit(distance, point, null, null, target.info(), null, false));
                        break;
                    }
                }
            }

            if (!options.blocks() && options.subLevels()) {
                for (Ship ship : ships) {
                    if (ship == own) continue;
                    BoundingBox3dc box = ship.box();
                    double reach = Math.sqrt(Math.pow(box.maxX() - box.minX(), 2) + Math.pow(box.maxY() - box.minY(), 2)
                        + Math.pow(box.maxZ() - box.minZ(), 2));
                    for (Vec3 sample : shipSamples(box)) {
                        Vector3d direction = aimAt(start, sample, axis, cosSpread, options.range());
                        if (direction == null) continue;
                        Hit hit = blocks(direction, Math.min(options.range(), start.distanceTo(sample) + reach));
                        if (hit != null && hit.subLevel() == ship.subLevel()) {
                            keepNearest(nearest, hit.asShip());
                            break;
                        }
                    }
                }
            }
            return nearest;
        }

        @Nullable
        Hit blocks(Vector3d direction, double range) {
            Hit best = walk(null, start, direction, 1.0, 0.0, range);
            for (Ship ship : ships) {
                double limit = best != null ? best.distance() : range;
                double[] span = slab(ship.box(), start, direction, limit);
                if (span == null) continue;
                Pose3dc pose = ship.pose();
                Vector3d entry = new Vector3d(direction).mul(span[0]).add(start);
                Vector3d origin = pose.transformPositionInverse(entry, new Vector3d());
                Vector3d localDirection = pose.transformNormalInverse(direction, new Vector3d());
                double scale = localDirection.length();
                if (scale < 1.0e-12) continue;
                localDirection.div(scale);
                Hit hit = walk(ship, origin, localDirection, scale, span[0], (span[1] - span[0]) * scale);
                if (hit != null && (best == null || hit.distance() < best.distance())) best = hit;
            }
            if (trace != null) {
                double length = best != null ? best.distance() : range;
                trace.add(startVec(), start.x + direction.x * length, start.y + direction.y * length,
                    start.z + direction.z * length, best == null ? LidarDebug.MISS : best.subLevel() != null ? LidarDebug.SHIP : LidarDebug.BLOCK);
            }
            return best;
        }

        @Nullable
        Hit accept(@Nullable Hit hit) {
            if (hit == null || hit.state() == null) return hit;
            boolean other = hit.subLevel() != null && (own == null || hit.subLevel() != own.subLevel());
            if (other && !options.subLevels()) return null;
            if (!options.blocks()) return other ? hit.asShip() : null;
            if (!options.fluids() && hit.state().getBlock() instanceof LiquidBlock) return null;
            if (options.exclude() != null && options.exclude().test(hit.state())) return null;
            if (options.include() != null && !options.include().test(hit.state())) return null;
            return hit;
        }

        void log(Vec3 point, byte kind) {
            if (trace != null) trace.add(startVec(), point.x, point.y, point.z, kind);
        }

        @Nullable
        private Hit walk(@Nullable Ship space, Vector3d o, Vector3d d, double scale, double offset, double tMax) {
            int x = (int) Math.floor(o.x), y = (int) Math.floor(o.y), z = (int) Math.floor(o.z);
            int sx = (int) Math.signum(d.x), sy = (int) Math.signum(d.y), sz = (int) Math.signum(d.z);
            double dx = sx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / d.x);
            double dy = sy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / d.y);
            double dz = sz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / d.z);
            double nx = sx == 0 ? Double.POSITIVE_INFINITY : ((sx > 0 ? x + 1 : x) - o.x) / d.x;
            double ny = sy == 0 ? Double.POSITIVE_INFINITY : ((sy > 0 ? y + 1 : y) - o.y) / d.y;
            double nz = sz == 0 ? Double.POSITIVE_INFINITY : ((sz > 0 ? z + 1 : z) - o.z) / d.z;
            boolean ownSpace = space == own;
            Long2ObjectOpenHashMap<LevelChunk> chunks = space != null ? space.chunks() : worldChunks;

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
                        chunk = chunks.get(ChunkPos.asLong(chunkX, chunkZ));
                        if (chunk == null) return null;
                    }
                    BlockState state = read(chunk, x, y, z);
                    if (!state.isAir() && !(ownSpace && x == self.getX() && y == self.getY() && z == self.getZ())) {
                        VoxelShape shape = shape(state);
                        if (!shape.isEmpty()) {
                            BlockPos pos = new BlockPos(x, y, z);
                            Vec3 from = new Vec3(o.x + d.x * t, o.y + d.y * t, o.z + d.z * t);
                            if (!options.precise()) return hit(space, o, scale, offset, from, pos, state);
                            Vec3 to = new Vec3(o.x + d.x * tMax, o.y + d.y * tMax, o.z + d.z * tMax);
                            BlockHitResult result = shape.clip(from, to, pos);
                            if (result != null) return hit(space, o, scale, offset, result.getLocation(), pos, state);
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

        private static BlockState read(LevelChunk chunk, int x, int y, int z) {
            try {
                LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                return section.hasOnlyAir() ? AIR : section.getBlockState(x & 15, y & 15, z & 15);
            } catch (RuntimeException e) {
                return AIR;
            }
        }

        private Hit hit(@Nullable Ship space, Vector3d o, double scale, double offset, Vec3 local, BlockPos pos, BlockState state) {
            double distance = offset + local.distanceTo(new Vec3(o.x, o.y, o.z)) / scale;
            Vec3 position = local;
            if (space != null) {
                Vector3d world = space.pose().transformPosition(new Vector3d(local.x, local.y, local.z), new Vector3d());
                position = new Vec3(world.x, world.y, world.z);
            }
            return new Hit(distance, position, pos, state, null, space != null ? space.subLevel() : null, false);
        }

        private VoxelShape shape(BlockState state) {
            return shapes.computeIfAbsent(state, this::computeShape);
        }

        private VoxelShape computeShape(BlockState state) {
            if (!options.seeThrough() && !state.canOcclude()) return Shapes.empty();
            FluidState fluid = state.getFluidState();
            VoxelShape fluidShape = !fluid.isEmpty() && (options.fluids() || fluid.is(FluidTags.LAVA))
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
