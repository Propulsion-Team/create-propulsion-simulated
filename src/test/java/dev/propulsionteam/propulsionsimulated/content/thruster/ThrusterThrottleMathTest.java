package dev.propulsionteam.propulsionsimulated.content.thruster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThrusterThrottleMathTest {
    @Test
    void normalizedThrottlePreservesValuesBetweenRedstoneSteps() {
        assertEquals(0.5f, ThrusterThrottleMath.clampNormalized(0.5d));
        assertEquals(0.123456f, ThrusterThrottleMath.clampNormalized(0.123456d));
        assertEquals(0.9999f, ThrusterThrottleMath.clampNormalized(0.9999d));
    }

    @Test
    void normalizedThrottleIsClampedToItsValidRange() {
        assertEquals(0.0f, ThrusterThrottleMath.clampNormalized(-1.0d));
        assertEquals(1.0f, ThrusterThrottleMath.clampNormalized(2.0d));
        assertEquals(0.0f, ThrusterThrottleMath.clampNormalized(Double.NaN));
    }
}
