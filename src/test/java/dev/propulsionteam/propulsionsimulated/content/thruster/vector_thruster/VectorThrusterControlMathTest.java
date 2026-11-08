package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void directionsUseTheConfiguredAngle() {
        for (double angle : new double[]{0.0d, 30.0d, 60.0d, 90.0d}) {
            var direction = VectorThrusterControlMath.direction(1.0f, 0.0f, angle);
            assertTrue(direction.isFinite());
            assertEquals(angle, Math.toDegrees(Math.acos(direction.z)), 1.0e-6d);
        }
    }

    @Test
    void angularSpeedDoesNotDependOnDisplacementOrDiagonalInput() {
        for (float[] target : new float[][]{{1.0f, 0.0f}, {1.0f, 1.0f}, {-0.5f, 0.25f}}) {
            var next = VectorThrusterControlMath.advance(0.0f, 0.0f, target[0], target[1], 30.0d, 60.0d, 0.05d);
            assertEquals(3.0d, angularDistance(0.0f, 0.0f, next.x(), next.y(), 30.0d), 1.0e-5d);
        }
    }

    @Test
    void stopsExactlyAtTheTargetWithoutOvershooting() {
        var next = VectorThrusterControlMath.advance(0.9f, -0.2f, 1.0f, -0.25f, 30.0d, 120.0d, 0.05d);
        assertEquals(1.0f, next.x());
        assertEquals(-0.25f, next.y());
    }

    @Test
    void subdividingTheTimestepPreservesTheResult() {
        var whole = VectorThrusterControlMath.advance(-0.8f, 0.6f, 0.9f, -0.4f, 30.0d, 120.0d, 0.05d);
        var subdivided = new VectorThrusterControlMath.Coordinates(-0.8f, 0.6f);
        for (int i = 0; i < 10; i++) {
            subdivided = VectorThrusterControlMath.advance(subdivided.x(), subdivided.y(), 0.9f, -0.4f,
                    30.0d, 120.0d, 0.005d);
        }
        assertEquals(whole.x(), subdivided.x(), 1.0e-6f);
        assertEquals(whole.y(), subdivided.y(), 1.0e-6f);
    }

    @Test
    void zeroAngleKeepsTheNozzleNeutral() {
        var next = VectorThrusterControlMath.advance(0.0f, 0.0f, 1.0f, -1.0f, 0.0d, 120.0d, 0.05d);
        var direction = VectorThrusterControlMath.direction(next.x(), next.y(), 0.0d);
        assertEquals(0.0d, direction.x);
        assertEquals(0.0d, direction.y);
        assertEquals(1.0d, direction.z);
    }

    @Test
    void oppositeDirectionsAtNinetyDegreesRotateThroughNeutral() {
        var next = VectorThrusterControlMath.advance(1.0f, 0.0f, -1.0f, 0.0f, 90.0d, 60.0d, 0.05d);
        assertEquals(3.0d, angularDistance(1.0f, 0.0f, next.x(), next.y(), 90.0d), 1.0e-5d);
        for (int i = 0; i < 60; i++) {
            next = VectorThrusterControlMath.advance(next.x(), next.y(), -1.0f, 0.0f, 90.0d, 60.0d, 0.05d);
            assertTrue(VectorThrusterControlMath.direction(next.x(), next.y(), 90.0d).isFinite());
        }
        assertEquals(-1.0f, next.x());
        assertEquals(0.0f, next.y());
    }

    @Test
    void renderingInterpolatesThePhysicalAngle() {
        for (double maxAngle : new double[]{30.0d, 90.0d}) {
            var start = VectorThrusterControlMath.direction(1.0f, 0.0f, maxAngle);
            var end = VectorThrusterControlMath.direction(-1.0f, 0.0f, maxAngle);
            var interpolated = VectorThrusterControlMath.interpolateDirection(1.0f, 0.0f, -1.0f, 0.0f,
                    maxAngle, 0.25f);
            assertTrue(interpolated.isFinite());
            assertEquals(start.angle(end) * 0.25d, start.angle(interpolated), 1.0e-6d);
        }
    }

    @Test
    void invalidTimestepsAndZeroSpeedDoNotMoveTheNozzle() {
        for (double deltaTime : new double[]{0.0d, -0.05d, Double.NaN, Double.POSITIVE_INFINITY}) {
            var next = VectorThrusterControlMath.advance(0.2f, -0.3f, 1.0f, 1.0f, 30.0d, 120.0d, deltaTime);
            assertEquals(0.2f, next.x());
            assertEquals(-0.3f, next.y());
        }
        var next = VectorThrusterControlMath.advance(0.2f, -0.3f, 1.0f, 1.0f, 30.0d, 0.0d, 0.05d);
        assertEquals(0.2f, next.x());
        assertEquals(-0.3f, next.y());
    }

    private static double angularDistance(float x, float y, float targetX, float targetY, double maxAngle) {
        var current = VectorThrusterControlMath.direction(x, y, maxAngle);
        var target = VectorThrusterControlMath.direction(targetX, targetY, maxAngle);
        return Math.toDegrees(Math.acos(Math.clamp(current.dot(target), -1.0d, 1.0d)));
    }

    private static void assertRoundTrip(double requested) {
        float coordinate = VectorThrusterControlMath.clampCoordinate(requested);
        float actual = VectorThrusterControlMath.coordinateFromSignals(
                VectorThrusterControlMath.positiveSignal(coordinate),
                VectorThrusterControlMath.negativeSignal(coordinate));
        assertEquals(coordinate, actual, 1.0e-6f);
    }
}
