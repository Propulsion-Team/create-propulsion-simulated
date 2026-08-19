package dev.propulsionteam.propulsionsimulated.content.thruster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.network.PropulsionPackets;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import dev.propulsionteam.propulsionsimulated.network.SyncThrusterFuelsPacket;

public class ThrusterFuelManager extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String DIRECTORY = "thruster_fuels";

    private static Map<Fluid, FluidThrusterProperties> fuelPropertiesMap = new HashMap<>();
    private static Map<Fluid, FluidThrusterProperties> scriptedFuelPropertiesMap = new HashMap<>();
    private static Map<ResourceLocation, FluidThrusterProperties> scriptedFuelPropertiesById = new HashMap<>();
    private static Set<ResourceLocation> removedFuelIds = new HashSet<>();
    private static Map<ResourceLocation, Float> fuelEfficiencyOverrides = new HashMap<>();
    private static Map<ResourceLocation, JsonElement> cachedThrusterFuelDatapack = null;

    public static Map<Fluid, FluidThrusterProperties> getFuelPropertiesMap() {
        Map<Fluid, FluidThrusterProperties> merged = new HashMap<>(fuelPropertiesMap);
        merged.putAll(scriptedFuelPropertiesMap);
        for (ResourceLocation removedFuelId : removedFuelIds) {
            Fluid removedFluid = BuiltInRegistries.FLUID.get(removedFuelId);
            if (removedFluid != null && removedFluid != Fluids.EMPTY) {
                merged.remove(removedFluid);
            }
        }
        return merged;
    }

    public static Set<ResourceLocation> getRemovedFuelIds() {
        return Set.copyOf(removedFuelIds);
    }

    public static Map<ResourceLocation, Float> getEfficiencyOverrides() {
        return Map.copyOf(fuelEfficiencyOverrides);
    }

    public ThrusterFuelManager() {
        super(GSON, DIRECTORY);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public static FluidThrusterProperties getProperties(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        fluid = FluidHelper.convertToStill(fluid);
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId != null && removedFuelIds.contains(fluidId)) {
            return null;
        }
        FluidThrusterProperties props = scriptedFuelPropertiesMap.get(fluid);
        if (props != null) {
            return props;
        }
        if (fluidId != null) {
            props = scriptedFuelPropertiesById.get(fluidId);
            if (props != null) {
                return props;
            }
        }
        return fuelPropertiesMap.get(fluid);
    }

    public static float getEfficiency(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return 1.0f;
        }

        // Normalize flowing variants (e.g. flowing_lava) to their source fluid ids.
        fluid = FluidHelper.convertToStill(fluid);

        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId == null) {
            return 1.0f;
        }

        return fuelEfficiencyOverrides.getOrDefault(fluidId, 1.0f);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> pObject, @Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        //Parse datapacks
        profiler.push(CreatePropulsion.ID + ":Loading_thruster_fuels");
        cachedThrusterFuelDatapack = new HashMap<>(pObject);
        fuelEfficiencyOverrides = getConfiguredEfficiencyOverrides();
        ParseResult parseResult = parseFuelProperties(cachedThrusterFuelDatapack);
        fuelPropertiesMap = parseResult.fuelMap();
        // Add fuels from config that might not be in datapacks
        ConfigMergeStats mergeStats = mergeConfigProperties(fuelPropertiesMap);
        logReloadSummary("datapack_reload", parseResult, mergeStats);
        profiler.pop();
        //Update clients (happens only on /reload as on server start server instance is still null)
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) {
            PropulsionPackets.sendToAll(SyncThrusterFuelsPacket.create(
                getFuelPropertiesMap(), getRemovedFuelIds(), getEfficiencyOverrides()));
        }
    }

    public static void updateClient(Map<ResourceLocation, FluidThrusterProperties> fuelMap,
                                    Set<ResourceLocation> removedFuelIdsFromServer,
                                    Map<ResourceLocation, Float> efficiencyOverridesFromServer) {
        Map<Fluid, FluidThrusterProperties> newClientMap = new HashMap<>();
        fuelMap.forEach((rl, props) -> {
            Fluid fluid = BuiltInRegistries.FLUID.get(rl);
            if (fluid != null) {
                newClientMap.put(fluid, props);
            }
        });
        fuelPropertiesMap = newClientMap;
        removedFuelIds = new HashSet<>(removedFuelIdsFromServer);
        fuelEfficiencyOverrides = new HashMap<>(efficiencyOverridesFromServer);
    }

    public static void clearScriptedFuels() {
        scriptedFuelPropertiesMap.clear();
        scriptedFuelPropertiesById.clear();
        removedFuelIds.clear();
        syncFuelDataToClients();
    }

    public static boolean registerScriptedFuel(String fluidId, Map<String, Object> settings) {
        if (settings == null) {
            LOGGER.warn("[{}] KubeJS fuel registration failed: settings object is null for '{}'.", CreatePropulsion.ID, fluidId);
            return false;
        }

        float thrustMultiplier = getFloatSetting(settings, "thrustMultiplier", "thrust_multiplier", 1.0f);
        float consumptionMultiplier = getFloatSetting(settings, "consumptionMultiplier", "consumption_multiplier", 1.0f);
        String particleName = getStringSetting(settings, "particle", "particle", "plume");
        List<String> overrideTextureIds = getStringListSetting(settings, "overrideTextures", "override_textures");
        Integer overrideColor = getColorSetting(settings, "overrideColor", "override_color");
        boolean useFluidColor = getBooleanSetting(settings, "useFluidColor", "use_fluid_color", false);

        ResourceLocation fluidLocation = ResourceLocation.tryParse(fluidId);
        if (fluidLocation == null) {
            LOGGER.warn("[{}] KubeJS fuel registration failed: invalid fluid id '{}'.", CreatePropulsion.ID, fluidId);
            return false;
        }
        return registerScriptedFuelInternal(
            fluidLocation,
            thrustMultiplier,
            consumptionMultiplier,
            particleName,
            overrideTextureIds,
            overrideColor,
            useFluidColor
        );
    }

    public static boolean overrideFuel(String fluidIdToOverride, Map<String, Object> settings) {
        return registerScriptedFuel(fluidIdToOverride, settings);
    }

    public static boolean removeFuel(String fuelIdToRemove) {
        ResourceLocation fluidLocation = ResourceLocation.tryParse(fuelIdToRemove);
        if (fluidLocation == null) {
            LOGGER.warn("[{}] KubeJS fuel removal failed: invalid fluid id '{}'.", CreatePropulsion.ID, fuelIdToRemove);
            return false;
        }
        removedFuelIds.add(fluidLocation);
        Fluid removedFluid = BuiltInRegistries.FLUID.get(fluidLocation);
        if (removedFluid != null && removedFluid != Fluids.EMPTY) {
            scriptedFuelPropertiesMap.remove(removedFluid);
        }
        scriptedFuelPropertiesById.remove(fluidLocation);
        syncFuelDataToClients();
        return true;
    }

    /**
     * Re-applies datapack thruster fuels using current common config (efficiency / burn rate / additional lines).
     * Called when {@code createpropulsion-common.toml} reloads without a full datapack reload.
     */
    public static void rebuildThrusterFuelsAfterCommonConfigReload() {
        if (cachedThrusterFuelDatapack == null) {
            return;
        }
        fuelEfficiencyOverrides = getConfiguredEfficiencyOverrides();
        ParseResult parseResult = parseFuelProperties(cachedThrusterFuelDatapack);
        fuelPropertiesMap = parseResult.fuelMap();
        ConfigMergeStats mergeStats = mergeConfigProperties(fuelPropertiesMap);
        logReloadSummary("common_config_reload", parseResult, mergeStats);
        syncFuelDataToClients();
    }

    private static ParseResult parseFuelProperties(@Nonnull Map<ResourceLocation, JsonElement> pObject) {
        Map<Fluid, FluidThrusterProperties> newMap = new HashMap<>();
        Map<ResourceLocation, Float> consumptionOverrides = getConfiguredConsumptionOverrides();
        int[] parsedEntries = {0};
        int[] skippedMissingModEntries = {0};
        int[] skippedMissingFluidEntries = {0};

        for (Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation file = entry.getKey();
            JsonElement json = entry.getValue();

            //Parse fuel def
            ThrusterFuelDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> {LOGGER.error("[{}] Failed to parse thruster fuel definition from {}: {}", CreatePropulsion.ID, file, error);})
                .ifPresent(definition -> {
                    //There is a fuel that requires a mod but the mod is not present
                    if (definition.requiredMod().isPresent() && !ModList.get().isLoaded(definition.requiredMod().get())) {
                        skippedMissingModEntries[0]++;
                        return;
                    }
                    Fluid fluid = definition.getFluid();
                    //Fluid is not in registry
                    if (fluid == Fluids.EMPTY) {
                        skippedMissingFluidEntries[0]++;
                        return;
                    }
                    //Successfully load fuel
                    FluidThrusterProperties properties = new FluidThrusterProperties(
                        definition.thrustMultiplier(), 
                        definition.consumptionMultiplier(),
                        definition.particle(),
                        definition.overrideTextures(),
                        definition.overrideColor().map(ThrusterFuelManager::sanitizeColor).orElse(null),
                        definition.useFluidColor());

                    ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
                    if (fluidId != null && consumptionOverrides.containsKey(fluidId)) {
                            properties = new FluidThrusterProperties(
                            properties.thrustMultiplier(),
                            consumptionOverrides.get(fluidId),
                            properties.particleType(),
                            properties.overrideTextures(),
                            properties.overrideColor(),
                            properties.useFluidColor());
                    }
                    newMap.put(fluid, properties);
                    parsedEntries[0]++;
                });
        }

        return new ParseResult(newMap, parsedEntries[0], skippedMissingModEntries[0], skippedMissingFluidEntries[0]);
    }

    private static Map<ResourceLocation, Float> getConfiguredEfficiencyOverrides() {
        Map<ResourceLocation, Float> overrides = new HashMap<>();
        // Base table: unified fuel properties list ("<fluid>=<efficiency>,<burnRate>").
        for (String rawEntry : PropulsionConfig.getFuelPropertiesOrDefault()) {
            if (rawEntry == null) {
                continue;
            }
            String entry = rawEntry.trim();
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                continue;
            }
            String fluidId = entry.substring(0, separator).trim();
            String valuePart = entry.substring(separator + 1).trim();
            String[] values = valuePart.split(",");
            if (values.length < 1) {
                continue;
            }
            ResourceLocation rl = ResourceLocation.tryParse(fluidId);
            if (rl == null) {
                continue;
            }
            try {
                float percent = Float.parseFloat(values[0].trim());
                if (percent >= 0.0f) {
                    overrides.put(rl, percent / 100.0f);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return overrides;
    }

    private static Map<ResourceLocation, Float> getConfiguredConsumptionOverrides() {
        Map<ResourceLocation, Float> overrides = new HashMap<>();
        // Base table: unified fuel properties list ("<fluid>=<efficiency>,<burnRate>").
        for (String rawEntry : PropulsionConfig.getFuelPropertiesOrDefault()) {
            if (rawEntry == null) {
                continue;
            }
            String entry = rawEntry.trim();
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                continue;
            }
            String fluidId = entry.substring(0, separator).trim();
            String valuePart = entry.substring(separator + 1).trim();
            String[] values = valuePart.split(",");
            if (values.length < 2) {
                continue;
            }
            ResourceLocation rl = ResourceLocation.tryParse(fluidId);
            if (rl == null) {
                continue;
            }
            try {
                float percent = Float.parseFloat(values[1].trim());
                if (percent >= 0.0f) {
                    overrides.put(rl, percent / 100.0f);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return overrides;
    }

    private static ConfigMergeStats mergeConfigProperties(Map<Fluid, FluidThrusterProperties> map) {
        Map<ResourceLocation, Float> consumptionOverrides = getConfiguredConsumptionOverrides();
        int addedByConfig = 0;
        int overriddenByConfig = 0;
        int missingConfigFluidIds = 0;

        for (ResourceLocation fluidId : consumptionOverrides.keySet()) {
            Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                LOGGER.warn("[{}] Ignoring fuel config override for '{}': fluid is not registered.", CreatePropulsion.ID, fluidId);
                missingConfigFluidIds++;
                continue;
            }
            
            if (!map.containsKey(fluid)) {
                map.put(fluid, new FluidThrusterProperties(
                    1.0f,
                    consumptionOverrides.get(fluidId),
                    ThrusterParticleType.PLUME,
                    List.of(),
                    null,
                    false
                ));
                addedByConfig++;
            } else {
                FluidThrusterProperties existing = map.get(fluid);
                if (java.lang.Math.abs(existing.consumptionMultiplier() - consumptionOverrides.get(fluidId)) > 1e-4) {
                     map.put(fluid, new FluidThrusterProperties(
                        existing.thrustMultiplier(),
                        consumptionOverrides.get(fluidId),
                        existing.particleType(),
                        existing.overrideTextures(),
                        existing.overrideColor(),
                        existing.useFluidColor()
                    ));
                    overriddenByConfig++;
                }
            }
        }

        return new ConfigMergeStats(addedByConfig, overriddenByConfig, missingConfigFluidIds);
    }

    private static void logReloadSummary(String context, ParseResult parseResult, ConfigMergeStats mergeStats) {
        int totalMergedEntries = fuelPropertiesMap.size();
        int scriptedEntries = scriptedFuelPropertiesMap.size();
        int removedEntries = removedFuelIds.size();
        LOGGER.info(
            "[{}] Thruster fuel reload ({}) complete: datapackParsed={}, datapackSkippedMissingMod={}, datapackSkippedMissingFluid={}, configAdded={}, configOverridden={}, configMissingFluid={}, mergedDatapackAndConfig={}, scriptedOverrides={}, removed={}",
            CreatePropulsion.ID,
            context,
            parseResult.parsedEntries(),
            parseResult.skippedMissingModEntries(),
            parseResult.skippedMissingFluidEntries(),
            mergeStats.addedByConfig(),
            mergeStats.overriddenByConfig(),
            mergeStats.missingConfigFluidIds(),
            totalMergedEntries,
            scriptedEntries,
            removedEntries
        );
    }

    private static void syncFuelDataToClients() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isRunning()) {
            PropulsionPackets.sendToAll(SyncThrusterFuelsPacket.create(
                getFuelPropertiesMap(), getRemovedFuelIds(), getEfficiencyOverrides()));
        }
    }

    private static Integer sanitizeColor(Integer color) {
        if (color == null) {
            return null;
        }
        return color & 0xFFFFFF;
    }

    private static List<ResourceLocation> parseTextureOverrides(List<String> overrideTextureIds) {
        if (overrideTextureIds == null || overrideTextureIds.isEmpty()) {
            return List.of();
        }
        return overrideTextureIds.stream()
            .map(ResourceLocation::tryParse)
            .filter(rl -> rl != null)
            .toList();
    }

    private static float getFloatSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey, float fallback) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String getStringSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey, String fallback) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return fallback;
        }
        return value.toString();
    }

    private static List<String> getStringListSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    private static Integer getColorSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return sanitizeColor(number.intValue());
        }
        String str = value.toString().trim().toLowerCase(Locale.ROOT);
        if (str.startsWith("#")) {
            str = str.substring(1);
        } else if (str.startsWith("0x")) {
            str = str.substring(2);
        }
        try {
            return sanitizeColor(Integer.parseInt(str, 16));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean getBooleanSetting(Map<String, Object> settings, String camelCaseKey, String snakeCaseKey, boolean fallback) {
        Object value = settings.containsKey(camelCaseKey) ? settings.get(camelCaseKey) : settings.get(snakeCaseKey);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    @SuppressWarnings("deprecation")
    private static boolean registerScriptedFuelInternal(ResourceLocation fluidId, float thrustMultiplier, float consumptionMultiplier, String particleName, List<String> overrideTextureIds, Integer overrideColor, boolean useFluidColor) {
        Fluid fluid = FluidHelper.convertToStill(BuiltInRegistries.FLUID.get(fluidId));
        if (fluid == null || fluid == Fluids.EMPTY) {
            LOGGER.warn("[{}] KubeJS fuel registration failed: fluid '{}' is not registered.", CreatePropulsion.ID, fluidId);
            return false;
        }
        ThrusterParticleType particleType = ThrusterParticleType.fromString(particleName);
        List<ResourceLocation> textureOverrides = parseTextureOverrides(overrideTextureIds);
        FluidThrusterProperties properties = new FluidThrusterProperties(thrustMultiplier, consumptionMultiplier, particleType, textureOverrides, sanitizeColor(overrideColor), useFluidColor);
        scriptedFuelPropertiesMap.put(fluid, properties);
        scriptedFuelPropertiesById.put(fluidId, properties);
        removedFuelIds.remove(fluidId);
        syncFuelDataToClients();
        return true;
    }

    private record ParseResult(
        Map<Fluid, FluidThrusterProperties> fuelMap,
        int parsedEntries,
        int skippedMissingModEntries,
        int skippedMissingFluidEntries
    ) {
        private ParseResult {
            fuelMap = Objects.requireNonNull(fuelMap);
        }
    }

    private record ConfigMergeStats(
        int addedByConfig,
        int overriddenByConfig,
        int missingConfigFluidIds
    ) {
    }
}
