package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import org.joml.Vector3d;

public final class VectorThrusterControlMath {
    private VectorThrusterControlMath() {}

    public static float clampCoordinate(double coordinate) {
        if (Double.isNaN(coordinate)) {
            return 0.0f;
        }
        return (float) Math.clamp(coordinate, -1.0d, 1.0d);
    }

    public static float positiveSignal(float coordinate) {
        return coordinate > 0.0f ? coordinate * 15.0f : 0.0f;
    }

    public static float negativeSignal(float coordinate) {
        return coordinate < 0.0f ? -coordinate * 15.0f : 0.0f;
    }

    public static float coordinateFromSignals(float positiveSignal, float negativeSignal) {
        return Math.clamp((positiveSignal - negativeSignal) / 15.0f, -1.0f, 1.0f);
    }

    public record Coordinates(float x, float y) {}

    public static Vector3d direction(float x, float y, double maxAngleDegrees) {
        if (maxAngleDegrees == 0.0d) return new Vector3d(0.0d, 0.0d, 1.0d);
        double angle = Math.toRadians(maxAngleDegrees);
        double sin = Math.sin(angle);
        return new Vector3d(x * sin, y * sin, Math.cos(angle)).normalize();
    }

    public static Coordinates advance(float x, float y, float targetX, float targetY,
                                      double maxAngleDegrees, double degreesPerSecond, double deltaTime) {
        if (!Double.isFinite(deltaTime) || deltaTime <= 0.0d) return new Coordinates(x, y);
        if (maxAngleDegrees == 0.0d) return new Coordinates(targetX, targetY);
        double step = Math.toRadians(degreesPerSecond) * deltaTime;
        if (step <= 0.0d) return new Coordinates(x, y);

        Vector3d current = direction(x, y, maxAngleDegrees);
        Vector3d target = direction(targetX, targetY, maxAngleDegrees);
        if (current.angle(target) <= step) return new Coordinates(targetX, targetY);
        rotateTowards(current, target, step);
        double scale = Math.cos(Math.toRadians(maxAngleDegrees))
                / (current.z * Math.sin(Math.toRadians(maxAngleDegrees)));
        return new Coordinates(clampCoordinate(current.x * scale), clampCoordinate(current.y * scale));
    }

    public static Vector3d interpolateDirection(float x, float y, float targetX, float targetY,
                                                 double maxAngleDegrees, float partialTick) {
        Vector3d current = direction(x, y, maxAngleDegrees);
        Vector3d target = direction(targetX, targetY, maxAngleDegrees);
        return rotateTowards(current, target, current.angle(target) * Math.clamp(partialTick, 0.0f, 1.0f));
    }

    private static Vector3d rotateTowards(Vector3d current, Vector3d target, double step) {
        double dot = Math.clamp(current.dot(target), -1.0d, 1.0d);
        double angle = Math.acos(dot);
        if (angle <= step || angle < 1.0e-10d) return current.set(target);

        Vector3d tangent = new Vector3d(target).fma(-dot, current);
        if (tangent.lengthSquared() < 1.0e-20d) {
            tangent.set(0.0d, 0.0d, 1.0d).fma(-current.z, current);
        }
        tangent.normalize();
        return current.mul(Math.cos(step)).fma(Math.sin(step), tangent).normalize();
    }
}
