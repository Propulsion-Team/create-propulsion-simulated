package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorThrusterControlMathTest {
    @Test
    void coordinatesBetweenRedstoneStepsRemainPrecise() {
        assertRoundTrip(0.5d);
        assertRoundTrip(0.123456d);
        assertRoundTrip(-0.9999d);
    }

    @Test
    void coordinatesAreClampedToTheirValidRange() {
        assertEquals(-1.0f, VectorThrusterControlMath.clampCoordinate(-2.0d));
        assertEquals(1.0f, VectorThrusterControlMath.clampCoordinate(2.0d));
        assertEquals(0.0f, VectorThrusterControlMath.clampCoordinate(Double.NaN));
    }

    private static void assertRoundTrip(double requested) {
        float coordinate = VectorThrusterControlMath.clampCoordinate(requested);
        float actual = VectorThrusterControlMath.coordinateFromSignals(
                VectorThrusterControlMath.positiveSignal(coordinate),
                VectorThrusterControlMath.negativeSignal(coordinate));
        assertEquals(coordinate, actual, 1.0e-6f);
    }
}
