package dev.propulsionteam.propulsionsimulated.particles;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class ParticleCollisionMath {
    private ParticleCollisionMath() {}

    public static Hit sweep(Vector3dc start, Vector3dc movement,
                            double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ, double radius) {
        double entry = 0.0d;
        double exit = 1.0d;
        int normalAxis = -1;
        double normalSign = 0.0d;
        for (int axis = 0; axis < 3; axis++) {
            double min = (axis == 0 ? minX : axis == 1 ? minY : minZ) - radius;
            double max = (axis == 0 ? maxX : axis == 1 ? maxY : maxZ) + radius;
            double origin = start.get(axis);
            double delta = movement.get(axis);
            if (Math.abs(delta) < 1.0e-12d) {
                if (origin <= min || origin >= max) return null;
                continue;
            }
            double near = (min - origin) / delta;
            double far = (max - origin) / delta;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            if (near >= entry) {
                entry = near;
                normalAxis = axis;
                normalSign = delta > 0.0d ? -1.0d : 1.0d;
            }
            exit = Math.min(exit, far);
            if (entry > exit) return null;
        }
        if (normalAxis < 0 || exit < 0.0d) return null;
        return new Hit(entry, new Vector3d().setComponent(normalAxis, normalSign));
    }

    public record Hit(double fraction, Vector3d normal) {}
}
