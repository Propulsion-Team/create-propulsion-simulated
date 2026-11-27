package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class LidarPeripheral implements IPeripheral {
    private final LidarBlockEntity lidar;

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

    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> scan(Optional<Map<?, ?>> options) throws LuaException {
        Map<?, ?> table = options.orElse(Map.of());
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
        List<Map<String, Object>> out = new ArrayList<>();
        for (LidarRaycast.Hit hit : lidar.scan(parsed, spread, rays)) out.add(describe(hit, blockState));
        return out;
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
        Predicate<BlockState> any = state -> false;
        for (Object entry : list.values()) {
            if (!(entry instanceof String name)) throw new LuaException(key + " entries must be strings");
            boolean tag = name.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? name.substring(1) : name);
            if (id == null) throw new LuaException("Invalid id '" + name + "' in " + key);
            if (tag) {
                TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, id);
                any = any.or(state -> state.is(tagKey));
            } else {
                Block block = BuiltInRegistries.BLOCK.getOptional(id)
                    .orElseThrow(() -> new LuaException("Unknown block '" + name + "' in " + key));
                any = any.or(state -> state.is(block));
            }
        }
        return any;
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
            Entity entity = hit.entity();
            out.put("type", entity instanceof Player ? "player" : entity instanceof LivingEntity ? "mob" : "entity");
            Map<String, Object> info = new HashMap<>();
            info.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
            info.put("position", LuaValues.vector(new Vector3d(entity.getX(), entity.getY(), entity.getZ())));
            if (entity instanceof Player player) info.put("name", player.getGameProfile().getName());
            else if (entity.hasCustomName()) info.put("name", entity.getCustomName().getString());
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
