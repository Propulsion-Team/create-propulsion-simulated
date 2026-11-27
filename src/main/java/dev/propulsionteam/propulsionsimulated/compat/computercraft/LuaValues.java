package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import org.joml.Matrix3dc;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class LuaValues {
    private LuaValues() {}

    enum Frame { WORLD, LOCAL }

    static Map<String, Object> vector(Vector3dc v) {
        return Map.of("x", v.x(), "y", v.y(), "z", v.z());
    }

    static Map<String, Object> quaternion(Quaterniondc q) {
        return Map.of("x", q.x(), "y", q.y(), "z", q.z(), "w", q.w());
    }

    static List<List<Double>> matrix(Matrix3dc m) {
        Vector3d r0 = m.getRow(0, new Vector3d()), r1 = m.getRow(1, new Vector3d()), r2 = m.getRow(2, new Vector3d());
        return List.of(List.of(r0.x, r0.y, r0.z), List.of(r1.x, r1.y, r1.z), List.of(r2.x, r2.y, r2.z));
    }

    static Frame frame(Optional<String> name, Frame fallback) throws LuaException {
        if (name.isEmpty()) return fallback;
        return switch (name.get().toLowerCase(Locale.ROOT)) {
            case "world" -> Frame.WORLD;
            case "local" -> Frame.LOCAL;
            default -> throw new LuaException("Unknown frame '" + name.get() + "' (expected \"world\" or \"local\")");
        };
    }

    static Vector3d readVector(IArguments args, int index) throws LuaException {
        return toVector(args.getTable(index), "argument #" + (index + 1));
    }

    static Vector3d toVector(Object value, String name) throws LuaException {
        if (!(value instanceof Map<?, ?> table)) throw new LuaException(name + " must be a vector");
        Vector3d v = new Vector3d(number(table, "x", name), number(table, "y", name), number(table, "z", name));
        if (!v.isFinite()) throw new LuaException(name + " must be finite");
        return v;
    }

    private static double number(Map<?, ?> table, String key, String name) throws LuaException {
        if (table.get(key) instanceof Number n) return n.doubleValue();
        throw new LuaException(name + " field '" + key + "' must be a number");
    }
}
