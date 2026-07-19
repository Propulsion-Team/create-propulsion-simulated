package dev.propulsionteam.propulsionsimulated.content.thruster;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Pure plume sampling helpers shared by server emission and client trail rendering. */
public final class PlumeTrailMath {
    public static final double ANGLE_ON_DEGREES = 4.0d;
    public static final double ANGLE_OFF_DEGREES = 2.0d;
    public static final double DISTANCE_ON_BLOCKS = 0.5d;
    public static final double DISTANCE_OFF_BLOCKS = 0.3d;
    public static final int MAX_SUB_TICK_SAMPLES = 4;
    public static final int MAX_TRAIL_NODES = 32;
    public static final int TRAIL_LIFETIME_TICKS = 8;

    private PlumeTrailMath() {}

    public static double angleDegrees(Vec3 a, Vec3 b) {
        if (a.lengthSqr() < 1.0e-12d || b.lengthSqr() < 1.0e-12d) return 0.0d;
        double dot = Mth.clamp(a.normalize().dot(b.normalize()), -1.0d, 1.0d);
        return Math.toDegrees(Math.acos(dot));
    }

    /** Stable unit-vector interpolation, including nearly opposite inputs. */
    public static Vec3 slerpDirection(Vec3 from, Vec3 to, double t) {
        PlumeKinematics.Vector result = PlumeKinematics.slerp(vector(from), vector(to), t);
        return vec3(result);
    }

    public static Vec3 hermite(Vec3 p0, Vec3 velocity0, Vec3 p1, Vec3 velocity1, double t) {
        return vec3(PlumeKinematics.hermite(vector(p0), vector(velocity0), vector(p1), vector(velocity1), t));
    }

    public static int interpolationSteps(double angleDegrees, double distanceBlocks) {
        return PlumeKinematics.interpolationSteps(angleDegrees, distanceBlocks, MAX_SUB_TICK_SAMPLES);
    }

    public static int cappedEmissionCount(double desired, int cap) {
        if (cap <= 0 || !Double.isFinite(desired) || desired <= 0.0d) return 0;
        return Mth.clamp((int) Math.floor(desired), 0, cap);
    }

    public static EmissionBudget emissionBudget(double density, double carry, int cap) {
        PlumeKinematics.EmissionBudget result = PlumeKinematics.emissionBudget(density, carry, cap);
        return new EmissionBudget(result.count(), result.carry());
    }

    public static float activationTarget(double angleDegrees, double separationBlocks, boolean active) {
        return PlumeKinematics.activationTarget(angleDegrees, separationBlocks, active,
                ANGLE_ON_DEGREES, ANGLE_OFF_DEGREES, DISTANCE_ON_BLOCKS, DISTANCE_OFF_BLOCKS);
    }

    public static double integratedDragDistance(double firstStep, double friction, double ageTicks) {
        return PlumeKinematics.integratedDragDistance(firstStep, friction, ageTicks);
    }

    public static boolean isTrailNodeAlive(double birthTime, double now) {
        return now - birthTime <= TRAIL_LIFETIME_TICKS;
    }

    public record EmissionBudget(int count, double carry) {}

    private static PlumeKinematics.Vector vector(Vec3 value) {
        return new PlumeKinematics.Vector(value.x, value.y, value.z);
    }

    private static Vec3 vec3(PlumeKinematics.Vector value) {
        return new Vec3(value.x(), value.y(), value.z());
    }
}
