package dev.propulsionteam.propulsionsimulated.registries;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PropulsionDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreatePropulsion.ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidStack>> SUPER_BUCKET_FLUID =
            COMPONENTS.register("super_bucket_fluid", () -> DataComponentType.<FluidStack>builder()
                    .persistent(FluidStack.CODEC)
                    .networkSynchronized(FluidStack.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
