package dev.createpropulsionsimulated.client.sound;

import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterPlumeSpec;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionSoundEvents;
import net.minecraft.sounds.SoundEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Sound phases associated with a resolved plume style. */
public record ThrusterSoundProfile(
        Supplier<SoundEvent> startup,
        Supplier<SoundEvent> loop,
        @Nullable Supplier<SoundEvent> shutdown) {

    private static final ThrusterSoundProfile STANDARD = new ThrusterSoundProfile(
            PropulsionSoundEvents.THRUSTER_STARTUP,
            PropulsionSoundEvents.THRUSTER_LOOP,
            PropulsionSoundEvents.THRUSTER_OFF
    );
    private static final ThrusterSoundProfile ION = new ThrusterSoundProfile(
            PropulsionSoundEvents.ION_THRUSTER_STARTUP,
            PropulsionSoundEvents.ION_THRUSTER_LOOP,
            PropulsionSoundEvents.ION_THRUSTER_OFF
    );

    public static ThrusterSoundProfile forStyle(ThrusterPlumeSpec.Style style) {
        return switch (style) {
            case ION, VECTOR -> ION;
            case FIRE, SOLID, SUPERHEATED_SOLID, PLASMA -> STANDARD;
        };
    }

    @Nullable
    public SoundEvent shutdownEvent() {
        return shutdown == null ? null : shutdown.get();
    }
}
