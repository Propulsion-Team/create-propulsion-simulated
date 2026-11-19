package dev.propulsionteam.propulsionsimulated.content.platinum;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.foundation.fluid.FluidHelper;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

import com.google.gson.JsonElement;

public class CoralGeneratorFuelManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "coral_generator_fuels";

    private static Map<Fluid, CoralGeneratorFuelProperties> fuelPropertiesMap = new HashMap<>();
    private static Map<ResourceLocation, JsonElement> cachedCoralFuelDatapack = null;

    public CoralGeneratorFuelManager() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> data, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        profiler.push(CreatePropulsion.ID + ":loading_coral_generator_fuels");
        cachedCoralFuelDatapack = new HashMap<>(data);
        fuelPropertiesMap = parseFuelProperties(data);
        profiler.pop();
    }

    /** Re-merge coral FE/mb rates from common config without a datapack reload. */
    public static void rebuildCoralFuelsAfterCommonConfigReload() {
        if (cachedCoralFuelDatapack == null) {
            return;
        }
        fuelPropertiesMap = parseFuelProperties(cachedCoralFuelDatapack);
    }

    @Nullable
    public static CoralGeneratorFuelProperties getProperties(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return null;
        }
        fluid = FluidHelper.convertToStill(fluid);
        return fuelPropertiesMap.get(fluid);
    }

    private static Map<Fluid, CoralGeneratorFuelProperties> parseFuelProperties(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        Map<Fluid, CoralGeneratorFuelProperties> parsed = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation file = entry.getKey();
            JsonElement json = entry.getValue();

            CoralGeneratorFuelDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> LOGGER.error("[{}] Failed to parse coral generator fuel from {}: {}", CreatePropulsion.ID, file, error))
                .ifPresent(definition -> {
                    if (definition.requiredMod().isPresent() && !ModList.get().isLoaded(definition.requiredMod().get())) {
                        return;
                    }

                    Fluid fluid = FluidHelper.convertToStill(definition.getFluid());
                    if (fluid == Fluids.EMPTY) {
                        return;
                    }

                    int fePerMb = Math.max(0, definition.fePerMb());
                    parsed.put(fluid, new CoralGeneratorFuelProperties(fePerMb));
                });
        }

        applyConfigOverrides(parsed);
        return parsed;
    }

    private static void applyConfigOverrides(Map<Fluid, CoralGeneratorFuelProperties> parsed) {
        for (String rawEntry : PropulsionConfig.getCoralFuelConversionRatesOrDefault()) {
            if (rawEntry == null) {
                continue;
            }

            String entry = rawEntry.trim();
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                LOGGER.warn("[{}] Ignoring malformed coral config entry '{}'. Expected '<namespace:fluid>=<fe_per_mb>'.", CreatePropulsion.ID, rawEntry);
                continue;
            }

            String fluidId = entry.substring(0, separator).trim();
            String feText = entry.substring(separator + 1).trim();

            ResourceLocation fluidLocation = ResourceLocation.tryParse(fluidId);
            if (fluidLocation == null) {
                LOGGER.warn("[{}] Ignoring coral config entry with invalid fluid id '{}'.", CreatePropulsion.ID, rawEntry);
                continue;
            }

            int fePerMb;
            try {
                fePerMb = Integer.parseInt(feText);
            } catch (NumberFormatException e) {
                LOGGER.warn("[{}] Ignoring coral config entry with invalid FE value '{}'.", CreatePropulsion.ID, rawEntry);
                continue;
            }

            if (fePerMb < 0) {
                LOGGER.warn("[{}] Ignoring coral config entry with negative FE value '{}'.", CreatePropulsion.ID, rawEntry);
                continue;
            }

            Fluid fluid = BuiltInRegistries.FLUID.get(fluidLocation);
            fluid = FluidHelper.convertToStill(fluid);
            if (fluid == Fluids.EMPTY) {
                LOGGER.warn("[{}] Ignoring coral config entry for unknown fluid '{}'.", CreatePropulsion.ID, rawEntry);
                continue;
            }

            parsed.put(fluid, new CoralGeneratorFuelProperties(fePerMb));
        }
    }
}
