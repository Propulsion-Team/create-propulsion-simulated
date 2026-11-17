package dev.propulsionteam.propulsionsimulated.worldgen;

import com.mojang.serialization.MapCodec;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class WorldgenEnabledCondition implements ICondition {
    public static final WorldgenEnabledCondition INSTANCE = new WorldgenEnabledCondition();

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, CreatePropulsion.ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<WorldgenEnabledCondition>> CODEC =
            CONDITION_CODECS.register("worldgen_enabled", () -> MapCodec.unit(INSTANCE));

    private WorldgenEnabledCondition() {
    }

    public static void register(IEventBus modBus) {
        CONDITION_CODECS.register(modBus);
    }

    @Override
    public boolean test(IContext context) {
        return !PropulsionConfig.COMMON_SPEC.isLoaded() || !PropulsionConfig.DISABLE_WORLDGEN.get();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC.get();
    }

    @Override
    public String toString() {
        return CreatePropulsion.ID + ":worldgen_enabled";
    }
}
