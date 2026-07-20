package dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IonThrusterEnergyMathTest {
    @Test
    void emptyBufferCannotProduceThrustWhenDrainRoundsToZero() {
        assertEquals(0.0f, IonThrusterEnergyMath.poweredFraction(0, 0, 0));
    }

    @Test
    void fractionalDrainKeepsThrustContinuousWhileEnergyRemains() {
        assertEquals(1.0f, IonThrusterEnergyMath.poweredFraction(1, 0, 0));
    }

    @Test
    void partialPaymentScalesOutput() {
        assertEquals(0.25f, IonThrusterEnergyMath.poweredFraction(4, 4, 1));
    }

    @Test
    void failedPaymentCannotProduceThrust() {
        assertEquals(0.0f, IonThrusterEnergyMath.poweredFraction(4, 4, 0));
    }
}
