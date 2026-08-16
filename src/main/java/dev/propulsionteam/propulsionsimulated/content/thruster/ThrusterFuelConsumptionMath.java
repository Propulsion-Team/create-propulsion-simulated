package dev.propulsionteam.propulsionsimulated.content.thruster;

public final class ThrusterFuelConsumptionMath {
    private ThrusterFuelConsumptionMath() {
    }

    public static double accumulateDemand(double accumulatedDemand, double throttle, double consumptionMultiplier) {
        double safeAccumulatedDemand = Double.isFinite(accumulatedDemand) && accumulatedDemand > 0.0d
            ? accumulatedDemand
            : 0.0d;
        if (!Double.isFinite(throttle) || !Double.isFinite(consumptionMultiplier)) {
            return safeAccumulatedDemand;
        }
        return safeAccumulatedDemand + Math.max(0.0d, throttle) * Math.max(0.0d, consumptionMultiplier);
    }

    public static double requestedFuel(double fullThrottleFuelPerTick, double accumulatedDemand) {
        if (!Double.isFinite(fullThrottleFuelPerTick) || !Double.isFinite(accumulatedDemand)) {
            return 0.0d;
        }
        return Math.max(0.0d, fullThrottleFuelPerTick) * Math.max(0.0d, accumulatedDemand);
    }
}
