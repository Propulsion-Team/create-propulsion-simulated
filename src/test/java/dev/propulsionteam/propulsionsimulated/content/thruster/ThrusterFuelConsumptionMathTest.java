package dev.propulsionteam.propulsionsimulated.content.thruster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThrusterFuelConsumptionMathTest {
    @Test
    void changingThrottleConsumesOnlyTheDemandSampledForEachTick() {
        double demand = 0.0d;
        for (int redstoneLevel = 0; redstoneLevel <= 15; redstoneLevel++) {
            demand = ThrusterFuelConsumptionMath.accumulateDemand(demand, redstoneLevel / 15.0d, 1.0d);
        }

        assertEquals(8.0d, demand, 1.0e-10d);
        assertEquals(6.0d, ThrusterFuelConsumptionMath.requestedFuel(0.75d, demand), 1.0e-10d);
    }

    @Test
    void accumulatedDemandIsIndependentOfThrustUpdateBoundaries() {
        double firstWindow = 0.0d;
        double secondWindow = 0.0d;
        double combinedWindow = 0.0d;

        for (int redstoneLevel = 0; redstoneLevel <= 15; redstoneLevel++) {
            double throttle = redstoneLevel / 15.0d;
            combinedWindow = ThrusterFuelConsumptionMath.accumulateDemand(combinedWindow, throttle, 1.5d);
            if (redstoneLevel < 8) {
                firstWindow = ThrusterFuelConsumptionMath.accumulateDemand(firstWindow, throttle, 1.5d);
            } else {
                secondWindow = ThrusterFuelConsumptionMath.accumulateDemand(secondWindow, throttle, 1.5d);
            }
        }

        double splitConsumption = ThrusterFuelConsumptionMath.requestedFuel(0.75d, firstWindow)
            + ThrusterFuelConsumptionMath.requestedFuel(0.75d, secondWindow);
        assertEquals(ThrusterFuelConsumptionMath.requestedFuel(0.75d, combinedWindow), splitConsumption, 1.0e-10d);
    }
}
