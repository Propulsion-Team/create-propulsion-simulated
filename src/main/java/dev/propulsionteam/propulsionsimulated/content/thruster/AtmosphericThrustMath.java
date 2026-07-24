package dev.propulsionteam.propulsionsimulated.content.thruster;

final class AtmosphericThrustMath {
    private AtmosphericThrustMath() {
    }

    static double calculateFactor(boolean ionThruster, double airPressure, double strength) {
        double clampedPressure = org.joml.Math.clamp(0.0d, 1.0d, airPressure);
        double clampedStrength = org.joml.Math.clamp(0.0d, 2.0d, strength);

        if (ionThruster) {
            // Ion propulsion suffers strongly in dense air and reaches full output in vacuum.
            double target = org.joml.Math.clamp(0.2d, 1.0d, 1.0d - 0.8d * clampedPressure);
            return org.joml.Math.clamp(0.05d, 5.0d,
                    1.0d + (target - 1.0d) * clampedStrength);
        }

        // Chemical/rocket thrusters stay mostly constant and receive a mild vacuum bonus.
        double target = 1.0d + (1.0d - clampedPressure) * 0.15d;
        return org.joml.Math.clamp(0.05d, 5.0d,
                1.0d + (target - 1.0d) * clampedStrength);
    }
}
