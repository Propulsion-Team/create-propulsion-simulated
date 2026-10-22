package dev.propulsionteam.propulsionsimulated.particles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleScaleTest {
    private static final double EPSILON = 1.0e-8d;

    @Test
    void pymSpawnScaleIsAppliedOnce() {
        for (double scale : new double[]{0.0625d, 0.25d, 1.0d, 4.0d}) {
            ParticleScale state = new ParticleScale();
            state.multiply(scale);
            state.initialize(scale, true);
            assertEquals(scale, state.size(), EPSILON);
            assertEquals(scale, state.motion(), EPSILON);
            double storedVelocity = 0.5d * scale;
            assertEquals(0.5d * scale, storedVelocity / scale * state.motion(), EPSILON);
        }
    }

    @Test
    void attachedParticlesFollowResizingWithoutChangingBirthVelocityUnits() {
        ParticleScale state = new ParticleScale();
        state.multiply(0.0625d);
        state.initialize(0.0625d, true);
        assertEquals(64.0d, state.resize(4.0d), EPSILON);
        assertEquals(4.0d, state.size(), EPSILON);
        assertEquals(4.0d, state.motion(), EPSILON);
        assertEquals(2.0d, (0.5d * 0.0625d) / 0.0625d * state.motion(), EPSILON);
        state.resize(0.25d);
        assertEquals(0.25d, state.size(), EPSILON);
        assertEquals(0.25d, state.motion(), EPSILON);
    }

    @Test
    void sableProjectionWithoutPymIsNotScaledTwice() {
        ParticleScale state = new ParticleScale();
        state.initialize(0.25d, false);
        assertEquals(0.25d, state.size(), EPSILON);
        assertEquals(1.0d, state.motion(), EPSILON);
        state.resize(0.0625d);
        assertEquals(0.0625d, state.size(), EPSILON);
        assertEquals(0.25d, state.motion(), EPSILON);
    }

    @Test
    void worldParticlesAndInvalidScalesStayFinite() {
        for (double scale : new double[]{1.0d, 0.0d, -1.0d, Double.NaN, Double.POSITIVE_INFINITY}) {
            ParticleScale state = new ParticleScale();
            state.initialize(scale, false);
            assertEquals(1.0d, state.size(), EPSILON);
            assertEquals(1.0d, state.motion(), EPSILON);
            assertEquals(1.0d, state.resize(scale), EPSILON);
        }
    }
}
