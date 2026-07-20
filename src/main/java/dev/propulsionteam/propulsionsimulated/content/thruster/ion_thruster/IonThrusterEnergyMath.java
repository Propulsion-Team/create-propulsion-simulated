package dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster;

final class IonThrusterEnergyMath {
    private IonThrusterEnergyMath() {
    }

    /**
     * Resolves how much of the requested thrust was paid for by FE this update.
     * A rounded, sub-1 FE drain may keep thrust continuous only while some FE is
     * actually stored; an empty buffer must never produce free thrust.
     */
    static float poweredFraction(int energyBeforeDrain, int requestedDrain, int consumed) {
        if (energyBeforeDrain <= 0) {
            return 0.0f;
        }
        if (requestedDrain <= 0) {
            return 1.0f;
        }
        return org.joml.Math.clamp(0.0f, 1.0f, (float) consumed / (float) requestedDrain);
    }
}
