package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

/** Dependency-free vector conversion shared by ComputerCraft control and unit tests. */
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
}
