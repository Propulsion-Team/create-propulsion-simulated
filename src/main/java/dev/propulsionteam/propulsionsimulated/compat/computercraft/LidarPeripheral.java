package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.LuaValues.Frame;
import dev.propulsionteam.propulsionsimulated.content.lidar.LidarBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.lidar.LidarRaycast;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class LidarPeripheral implements IPeripheral {
    private static final String SCAN_EVENT = "lidar_scan";
    private static final AtomicInteger NEXT_SCAN = new AtomicInteger();
    private static final AtomicInteger ACTIVE_SCANS = new AtomicInteger();
    private static final int MAX_ACTIVE_SCANS = 64;
    private static final int MAX_FILTER_ENTRIES = 256;
    private static final long RESULT_TTL_NANOS = 5_000_000_000L;
    private final Map<Integer, ScanResult> results = new ConcurrentHashMap<>();
    private final AtomicLong scanStarted = new AtomicLong();
    private final LidarBlockEntity lidar;

    private record ScanResult(IComputerAccess computer, Object value, long expires) {}

    private record BlockFilter(Set<Block> blocks, List<TagKey<Block>> tags) implements Predicate<BlockState> {
        @Override
        public boolean test(BlockState state) {
            if (blocks.contains(state.getBlock())) return true;
            for (TagKey<Block> tag : tags) {
                if (state.is(tag)) return true;
            }
            return false;
        }
    }

    public LidarPeripheral(LidarBlockEntity lidar) {
        this.lidar = lidar;
    }

    @Override
    public String getType() {
        return "lidar";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof LidarPeripheral peripheral && peripheral.lidar == lidar;
    }

    @LuaFunction
    public final double getMaxRange() {
        return LidarRaycast.MAX_RANGE;
    }

    @LuaFunction
    public final double getMaxAngle() {
        return LidarRaycast.MAX_ANGLE_DEGREES;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getDirection(Optional<String> frame) throws LuaException {
        Vector3d normal = LidarRaycast.normal(lidar.getFacing());
        return LuaValues.vector(LuaValues.frame(frame, Frame.LOCAL) == Frame.WORLD ? orientation().transform(normal) : normal);
    }

    @LuaFunction
    public final int getMaxScanRays() {
        return LidarRaycast.MAX_SCAN_RAYS;
    }

    @LuaFunction(mainThread = true)
    public final Object raycast(Optional<Map<?, ?>> options) throws LuaException {
        Map<?, ?> table = options.orElse(Map.of());
        LidarRaycast.Hit hit = lidar.raycast(parse(table, 0.0));
        return hit == null ? null : describe(hit, flag(table, "blockState", true));
    }

    @LuaFunction
    public final MethodResult scan(IComputerAccess computer, ILuaContext context, Optional<Map<?, ?>> options)
        throws LuaException {
        Map<?, ?> table = options.orElse(Map.of());
        purge();
        long now = System.nanoTime();
        long started = scanStarted.get();
        if (started != 0 && now - started > RESULT_TTL_NANOS) release();
        if (!scanStarted.compareAndSet(0, now)) throw new LuaException("Lidar is busy");
        if (ACTIVE_SCANS.incrementAndGet() > MAX_ACTIVE_SCANS) {
            release();
            throw new LuaException("Lidar workers are busy, try again");
        }
        int id = NEXT_SCAN.incrementAndGet();
        try {
            context.issueMainThreadTask(() -> {
                try {
                    startScan(table).whenComplete((hits, error) -> {
                        release();
                        finish(computer, id, error != null ? new LuaException("Scan failed: " + error.getMessage()) : hits);
                    });
                } catch (LuaException e) {
                    release();
                    finish(computer, id, e);
                } catch (RuntimeException e) {
                    release();
                    finish(computer, id, new LuaException("Scan failed: " + e.getMessage()));
                }
                return null;
            });
        } catch (RuntimeException | LuaException e) {
            release();
            throw e;
        }
        return awaitScan(computer, id);
    }

    private void release() {
        if (scanStarted.getAndSet(0) != 0) ACTIVE_SCANS.decrementAndGet();
    }

    private void purge() {
        long now = System.nanoTime();
        results.values().removeIf(result -> now - result.expires() > 0);
    }

    @Override
    public void detach(IComputerAccess computer) {
        results.values().removeIf(result -> result.computer() == computer);
    }

    private void finish(IComputerAccess computer, int id, Object value) {
        results.put(id, new ScanResult(computer, value, System.nanoTime() + RESULT_TTL_NANOS));
        try {
            computer.queueEvent(SCAN_EVENT, id);
        } catch (RuntimeException e) {
            results.remove(id);
        }
    }

    private MethodResult awaitScan(IComputerAccess computer, int id) {
        return MethodResult.pullEvent(SCAN_EVENT, event -> {
            if (!(event.length > 1 && event[1] instanceof Number n && n.intValue() == id)) return awaitScan(computer, id);
            ScanResult result = results.get(id);
            if (result == null || result.computer() != computer) throw new LuaException("Scan result expired");
            results.remove(id);
            if (result.value() instanceof LuaException e) throw e;
            return MethodResult.of(result.value());
        });
    }

    private CompletableFuture<List<Map<String, Object>>> startScan(Map<?, ?> table) throws LuaException {
        double range = number(table, "range", LidarRaycast.MAX_RANGE);
        double spread;
        if (table.get("angle") != null) {
            double angle = number(table, "angle", LidarRaycast.MAX_ANGLE_DEGREES);
            if (!(angle >= 0)) throw new LuaException("angle must be 0 or more");
            spread = Math.toRadians(angle);
        } else if (table.get("radius") != null) {
            double radius = number(table, "radius", 0);
            if (!(radius >= 0)) throw new LuaException("radius must be 0 or more");
            spread = Math.atan2(radius, range);
        } else {
            spread = Math.toRadians(LidarRaycast.MAX_ANGLE_DEGREES);
        }
        int rays = (int) number(table, "rays", 256);
        if (rays < 1 || rays > LidarRaycast.MAX_SCAN_RAYS) {
            throw new LuaException("rays must be between 1 and " + LidarRaycast.MAX_SCAN_RAYS);
        }
        LidarRaycast.Options parsed = parse(table, spread);
        boolean blockState = flag(table, "blockState", true);
        return lidar.scan(parsed, spread, rays).thenApply(hits -> {
            List<Map<String, Object>> out = new ArrayList<>(hits.size());
            for (LidarRaycast.Hit hit : hits) out.add(describe(hit, blockState));
            return out;
        });
    }

    private Quaterniond orientation() {
        SubLevel ship = Sable.HELPER.getContaining(lidar.getLevel(), lidar.getBlockPos());
        return ship != null ? new Quaterniond(ship.logicalPose().orientation()) : new Quaterniond();
    }

    private LidarRaycast.Options parse(Map<?, ?> table, double spreadRadians) throws LuaException {
        LidarRaycast.Options d = LidarRaycast.Options.defaults();
        double range = number(table, "range", LidarRaycast.MAX_RANGE);
        if (!(range > 0 && range <= LidarRaycast.MAX_RANGE)) {
            throw new LuaException("range must be between 0 and " + LidarRaycast.MAX_RANGE);
        }

        Vector3d direction = null;
        if (table.get("direction") != null) {
            direction = LuaValues.toVector(table.get("direction"), "direction");
            if (direction.lengthSquared() < 1.0e-12) throw new LuaException("direction can't be zero");
            Object frameName = table.get("frame");
            if (frameName != null && !(frameName instanceof String)) throw new LuaException("frame must be a string");
            if (LuaValues.frame(Optional.ofNullable((String) frameName), Frame.LOCAL) == Frame.WORLD) {
                direction = orientation().transformInverse(direction);
            }
            direction.normalize();
        }
        Vector3d center = direction != null ? direction : LidarRaycast.normal(lidar.getFacing());
        if (!LidarRaycast.withinCone(lidar.getFacing(), center, spreadRadians)) {
            throw new LuaException("rays would be more than " + LidarRaycast.MAX_ANGLE_DEGREES
                + " degrees off the sensor axis; aim closer to it or reduce the angle");
        }

        return new LidarRaycast.Options(range, direction,
            flag(table, "fluids", d.fluids()), flag(table, "nonSolid", d.nonSolid()),
            flag(table, "seeThrough", d.seeThrough()), flag(table, "entities", d.entities()),
            flag(table, "players", d.players()), flag(table, "subLevels", d.subLevels()),
            flag(table, "ownShip", d.ownShip()), blocks(table, "include"), blocks(table, "exclude"),
            flag(table, "blocks", d.blocks()), flag(table, "precise", d.precise()));
    }

    private static double number(Map<?, ?> table, String key, double fallback) throws LuaException {
        Object value = table.get(key);
        if (value == null) return fallback;
        if (value instanceof Number n) return n.doubleValue();
        throw new LuaException(key + " must be a number");
    }

    private static boolean flag(Map<?, ?> table, String key, boolean fallback) throws LuaException {
        Object value = table.get(key);
        if (value == null) return fallback;
        if (value instanceof Boolean b) return b;
        throw new LuaException(key + " must be a boolean");
    }

    @Nullable
    private static Predicate<BlockState> blocks(Map<?, ?> table, String key) throws LuaException {
        Object value = table.get(key);
        if (value == null) return null;
        if (!(value instanceof Map<?, ?> list)) throw new LuaException(key + " must be a list of block ids or #tags");
        if (list.size() > MAX_FILTER_ENTRIES) throw new LuaException(key + " can have at most " + MAX_FILTER_ENTRIES + " entries");
        Set<Block> blocks = new HashSet<>();
        List<TagKey<Block>> tags = new ArrayList<>();
        for (Map.Entry<?, ?> item : list.entrySet()) {
            if (!(item.getKey() instanceof Number index && index.doubleValue() >= 1 && index.doubleValue() <= list.size()
                && index.doubleValue() == Math.rint(index.doubleValue()))) {
                throw new LuaException(key + " must be a list of block ids or #tags");
            }
            if (!(item.getValue() instanceof String name)) throw new LuaException(key + " entries must be strings");
            boolean tag = name.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? name.substring(1) : name);
            if (id == null) throw new LuaException("Invalid id '" + name + "' in " + key);
            if (tag) {
                tags.add(TagKey.create(Registries.BLOCK, id));
            } else {
                blocks.add(BuiltInRegistries.BLOCK.getOptional(id)
                    .orElseThrow(() -> new LuaException("Unknown block '" + name + "' in " + key)));
            }
        }
        return new BlockFilter(blocks, tags);
    }

    private static Map<String, Object> describe(LidarRaycast.Hit hit, boolean blockState) {
        Map<String, Object> out = new HashMap<>();
        out.put("distance", hit.distance());
        out.put("position", LuaValues.vector(new Vector3d(hit.position().x, hit.position().y, hit.position().z)));
        if (hit.state() != null) {
            out.put("type", hit.ship() ? "subLevel" : "block");
            Map<String, Object> block = new HashMap<>();
            block.put("id", BuiltInRegistries.BLOCK.getKey(hit.state().getBlock()).toString());
            if (blockState) {
                Map<String, Object> state = new HashMap<>();
                for (Property<?> property : hit.state().getProperties()) {
                    state.put(property.getName(), stateValue(hit.state(), property));
                }
                block.put("state", state);
            }
            out.put("block", block);
        } else if (hit.entity() != null) {
            LidarRaycast.EntityInfo entity = hit.entity();
            out.put("type", entity.kind());
            Map<String, Object> info = new HashMap<>();
            info.put("type", entity.type());
            info.put("position", LuaValues.vector(new Vector3d(entity.position().x, entity.position().y, entity.position().z)));
            if (entity.name() != null) info.put("name", entity.name());
            out.put("entity", info);
        }
        if (hit.subLevel() != null) {
            Map<String, Object> ship = new HashMap<>();
            ship.put("id", hit.subLevel().getUniqueId().toString());
            if (hit.subLevel().getName() != null) ship.put("name", hit.subLevel().getName());
            out.put("subLevel", ship);
        }
        return out;
    }

    private static <T extends Comparable<T>> String stateValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property)).toLowerCase(Locale.ROOT);
    }
}
