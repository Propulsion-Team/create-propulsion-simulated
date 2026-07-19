package dev.propulsionteam.propulsionsimulated.content.thruster;

/** Minecraft-independent plume math, kept separate so it can be covered by fast unit tests. */
public final class PlumeKinematics {
    private PlumeKinematics() {}

    public static Vector slerp(Vector from, Vector to, double t) {
        Vector a = from.normalized();
        Vector b = to.normalized();
        if (a.lengthSquared() < 1.0e-12d) return b;
        if (b.lengthSquared() < 1.0e-12d) return a;
        double dot = clamp(a.dot(b), -1.0d, 1.0d);
        if (dot > 0.9995d) return a.lerp(b, t).normalized();
        if (dot < -0.9995d) {
            Vector axis = Math.abs(a.x) < 0.8d ? a.cross(new Vector(1, 0, 0)) : a.cross(new Vector(0, 1, 0));
            axis = axis.normalized();
            double angle = Math.PI * t;
            return a.scale(Math.cos(angle)).add(axis.cross(a).scale(Math.sin(angle))).normalized();
        }
        double theta = Math.acos(dot);
        double sinTheta = Math.sin(theta);
        return a.scale(Math.sin((1.0d - t) * theta) / sinTheta)
                .add(b.scale(Math.sin(t * theta) / sinTheta)).normalized();
    }

    public static Vector hermite(Vector p0, Vector velocity0, Vector p1, Vector velocity1, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return p0.scale(2 * t3 - 3 * t2 + 1)
                .add(velocity0.scale(t3 - 2 * t2 + t))
                .add(p1.scale(-2 * t3 + 3 * t2))
                .add(velocity1.scale(t3 - t2));
    }

    public static int interpolationSteps(double angleDegrees, double distanceBlocks, int maximum) {
        int angular = (int) Math.ceil(angleDegrees / 1.5d);
        int positional = (int) Math.ceil(distanceBlocks / 0.15d);
        return (int) clamp(Math.max(1, Math.max(angular, positional)), 1, maximum);
    }

    public static EmissionBudget emissionBudget(double density, double carry, int cap) {
        double desired = Math.max(0.0d, density) + clamp(carry, 0.0d, 0.999999d);
        int count = cap <= 0 || !Double.isFinite(desired) ? 0 : (int) clamp(Math.floor(desired), 0, cap);
        double nextCarry = cap > 0 && desired < cap ? desired - Math.floor(desired) : 0.0d;
        return new EmissionBudget(count, nextCarry);
    }

    public static float activationTarget(double angleDegrees, double separationBlocks, boolean active,
                                         double angleOn, double angleOff, double distanceOn, double distanceOff) {
        double angleThreshold = active ? angleOff : angleOn;
        double distanceThreshold = active ? distanceOff : distanceOn;
        double severity = Math.max(angleDegrees / angleThreshold, separationBlocks / distanceThreshold);
        return (float) clamp((severity - 0.75d) / 0.75d, 0.0d, 1.0d);
    }

    public static double integratedDragDistance(double firstStep, double friction, double ageTicks) {
        if (ageTicks <= 0.0d) return 0.0d;
        if (Math.abs(1.0d - friction) < 1.0e-8d) return firstStep * ageTicks;
        return firstStep * (1.0d - Math.pow(friction, ageTicks)) / (1.0d - friction);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record EmissionBudget(int count, double carry) {}

    public record Vector(double x, double y, double z) {
        public Vector add(Vector other) { return new Vector(x + other.x, y + other.y, z + other.z); }
        public Vector scale(double scale) { return new Vector(x * scale, y * scale, z * scale); }
        public Vector lerp(Vector other, double t) { return scale(1.0d - t).add(other.scale(t)); }
        public double dot(Vector other) { return x * other.x + y * other.y + z * other.z; }
        public Vector cross(Vector other) {
            return new Vector(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x);
        }
        public double lengthSquared() { return dot(this); }
        public double length() { return Math.sqrt(lengthSquared()); }
        public Vector normalized() { double length = length(); return length < 1.0e-12d ? this : scale(1.0d / length); }
    }
}
