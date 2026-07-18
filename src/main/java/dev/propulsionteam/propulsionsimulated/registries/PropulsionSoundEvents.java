package dev.propulsionteam.propulsionsimulated.registries;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PropulsionSoundEvents {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, CreatePropulsion.ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> THRUSTER_STARTUP = register("thruster_startup");
    public static final DeferredHolder<SoundEvent, SoundEvent> THRUSTER_LOOP = SOUND_EVENTS.register(
        "thruster_loop",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "thruster_loop"))
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> THRUSTER_OFF = register("thruster_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> ION_THRUSTER_STARTUP = register("ion_thruster_startup");
    public static final DeferredHolder<SoundEvent, SoundEvent> ION_THRUSTER_LOOP = register("ion_thruster_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ION_THRUSTER_OFF = register("ion_thruster_off");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(
            name,
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, name))
        );
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}
