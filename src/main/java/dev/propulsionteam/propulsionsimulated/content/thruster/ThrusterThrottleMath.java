package dev.propulsionteam.propulsionsimulated.content.thruster;

/** Dependency-free throttle conversion shared by ComputerCraft control and unit tests. */
public final class ThrusterThrottleMath {
    private ThrusterThrottleMath() {}

    public static float clampNormalized(double normalized) {
        if (Double.isNaN(normalized) || normalized <= 0.0d) {
            return 0.0f;
        }
        if (normalized >= 1.0d) {
            return 1.0f;
        }
        return (float) normalized;
    }
}
