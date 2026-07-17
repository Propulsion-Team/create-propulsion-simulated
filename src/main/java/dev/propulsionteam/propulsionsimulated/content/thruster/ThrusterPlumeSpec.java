package dev.propulsionteam.propulsionsimulated.content.thruster;

import net.minecraft.core.particles.ParticleOptions;

/** The single resolved description of an engine's plume for both render modes. */
public record ThrusterPlumeSpec(boolean active, float power, ParticleOptions particle, Style style) {
    public enum Style {
        FIRE, SOLID, SUPERHEATED_SOLID, ION, VECTOR, PLASMA
    }

    public static ThrusterPlumeSpec inactive(Style style) {
        return new ThrusterPlumeSpec(false, 0.0f, null, style);
    }
}
