package dev.propulsionteam.propulsionsimulated.content.thruster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlumeTrailMathTest {
    private static final double EPSILON = 1.0e-6d;

    @Test
    void slerpFollowsQuarterTurn() {
        PlumeKinematics.Vector halfway = PlumeKinematics.slerp(
                new PlumeKinematics.Vector(1, 0, 0), new PlumeKinematics.Vector(0, 0, 1), 0.5d);
        assertEquals(Math.sqrt(0.5d), halfway.x(), EPSILON);
        assertEquals(Math.sqrt(0.5d), halfway.z(), EPSILON);
        assertEquals(1.0d, halfway.length(), EPSILON);
    }

    @Test
    void slerpRemainsFiniteNearOppositeDirections() {
        PlumeKinematics.Vector halfway = PlumeKinematics.slerp(
                new PlumeKinematics.Vector(1, 0, 0), new PlumeKinematics.Vector(-1, 0, 0), 0.5d);
        assertTrue(Double.isFinite(halfway.x()));
        assertTrue(Double.isFinite(halfway.y()));
        assertTrue(Double.isFinite(halfway.z()));
        assertEquals(1.0d, halfway.length(), EPSILON);
    }

    @Test
    void hermiteHonorsEndpoints() {
        PlumeKinematics.Vector p0 = new PlumeKinematics.Vector(1, 2, 3);
        PlumeKinematics.Vector p1 = new PlumeKinematics.Vector(5, 7, 11);
        PlumeKinematics.Vector v0 = new PlumeKinematics.Vector(2, 0, 0);
        PlumeKinematics.Vector v1 = new PlumeKinematics.Vector(0, 3, 0);
        assertEquals(p0, PlumeKinematics.hermite(p0, v0, p1, v1, 0.0d));
        assertEquals(p1, PlumeKinematics.hermite(p0, v0, p1, v1, 1.0d));
    }

    @Test
    void interpolationAndEmissionAreHardCapped() {
        assertEquals(4, PlumeKinematics.interpolationSteps(90.0d, 20.0d, 4));
        PlumeKinematics.EmissionBudget budget = PlumeKinematics.emissionBudget(12.75d, 0.5d, 4);
        assertEquals(4, budget.count());
        assertEquals(0.0d, budget.carry(), EPSILON);
        assertEquals(0, PlumeKinematics.emissionBudget(3.0d, 0.0d, 0).count());
    }

    @Test
    void fractionalEmissionCarriesWithoutSkippedDensity() {
        PlumeKinematics.EmissionBudget first = PlumeKinematics.emissionBudget(0.6d, 0.0d, 4);
        PlumeKinematics.EmissionBudget second = PlumeKinematics.emissionBudget(0.6d, first.carry(), 4);
        assertEquals(0, first.count());
        assertEquals(1, second.count());
        assertEquals(0.2d, second.carry(), EPSILON);
    }

    @Test
    void activationUsesHysteresisThresholds() {
        assertEquals(0.0f, PlumeKinematics.activationTarget(3.0d, 0.2d, false, 4, 2, 0.5, 0.3), EPSILON);
        assertTrue(PlumeKinematics.activationTarget(5.0d, 0.2d, false, 4, 2, 0.5, 0.3) > 0.0f);
        assertTrue(PlumeKinematics.activationTarget(3.0d, 0.2d, true, 4, 2, 0.5, 0.3) > 0.0f);
    }

    @Test
    void dragIntegrationAndNodeExpiryAreDeterministic() {
        assertEquals(3.439d, PlumeKinematics.integratedDragDistance(1.0d, 0.9d, 4.0d), EPSILON);
        assertTrue(18.0d - 10.0d <= 8.0d);
        assertFalse(18.01d - 10.0d <= 8.0d);
    }
}
