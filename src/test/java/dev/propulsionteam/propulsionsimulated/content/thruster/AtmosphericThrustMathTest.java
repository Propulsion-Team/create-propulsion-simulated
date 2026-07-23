package dev.propulsionteam.propulsionsimulated.content.thruster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AtmosphericThrustMathTest {
    @Test
    void vacuumNeverDisablesChemicalThrusters() {
        double factor = AtmosphericThrustMath.calculateFactor(false, 0.0d, 1.0d);

        assertEquals(1.15d, factor, 1.0e-9d);
        assertTrue(factor > 0.0d);
    }

    @Test
    void vacuumAllowsFullIonThrust() {
        assertEquals(1.0d,
                AtmosphericThrustMath.calculateFactor(true, 0.0d, 1.0d),
                1.0e-9d);
    }

    @Test
    void denseAtmosphereReducesIonThrustWithoutHardCutoff() {
        assertEquals(0.2d,
                AtmosphericThrustMath.calculateFactor(true, 1.0d, 1.0d),
                1.0e-9d);
    }

    @Test
    void disabledStrengthLeavesThrustUnchanged() {
        assertEquals(1.0d,
                AtmosphericThrustMath.calculateFactor(true, 1.0d, 0.0d),
                1.0e-9d);
        assertEquals(1.0d,
                AtmosphericThrustMath.calculateFactor(false, 0.0d, 0.0d),
                1.0e-9d);
    }

    @Test
    void pressureIsClampedToPhysicalRange() {
        assertEquals(
                AtmosphericThrustMath.calculateFactor(false, 0.0d, 1.0d),
                AtmosphericThrustMath.calculateFactor(false, -10.0d, 1.0d),
                1.0e-9d);
        assertEquals(
                AtmosphericThrustMath.calculateFactor(true, 1.0d, 1.0d),
                AtmosphericThrustMath.calculateFactor(true, 10.0d, 1.0d),
                1.0e-9d);
    }
}
