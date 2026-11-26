package dev.propulsionteam.propulsionsimulated;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionDefaultStress;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionItemToggles;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PropulsionConfig {
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    public enum PlumeRenderMode {
        PARTICLE,
        SHADER,
        SHADER_2
    }

    // ── Common: thrusters ───────────────────────────────────────────────────
    public static final ModConfigSpec.DoubleValue THRUST_UNITS_PER_PN;
    public static final ModConfigSpec.DoubleValue VECTOR_CORE_OUTPUT_PN;
    public static final ModConfigSpec.DoubleValue NOZZLE_OFFSET_FROM_CENTER;
    public static final ModConfigSpec.IntValue OBSTRUCTION_SCAN_LENGTH;
    public static final ModConfigSpec.BooleanValue OBSTRUCTION_IGNORE_OTHER_SUBLEVELS;
    public static final ModConfigSpec.BooleanValue DAMAGE_ENTITIES;
    public static final ModConfigSpec.BooleanValue USE_ATMOSPHERIC_PRESSURE;
    public static final ModConfigSpec.DoubleValue ATMOSPHERIC_PRESSURE_AMOUNT;

    public static final ModConfigSpec.DoubleValue BASE_THRUST;
    public static final ModConfigSpec.IntValue FUEL_TANK_CAPACITY_MB;
    public static final ModConfigSpec.DoubleValue FUEL_MB_PER_TICK_AT_FULL_THROTTLE;

    public static final ModConfigSpec.DoubleValue MULTIBLOCK_2X_THRUST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_3X_THRUST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_2X_FUEL_COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_3X_FUEL_COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_2X_OXIDIZER_COST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_3X_OXIDIZER_COST_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue ION_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.IntValue ION_THRUSTER_ENERGY_CAPACITY_FE;
    public static final ModConfigSpec.DoubleValue ION_THRUSTER_FE_PER_TICK_AT_FULL_THROTTLE;
    public static final ModConfigSpec.DoubleValue ION_MULTIBLOCK_2X_THRUST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ION_MULTIBLOCK_3X_THRUST_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue VECTOR_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue VECTOR_THRUSTER_MAX_ANGLE;
    public static final ModConfigSpec.DoubleValue VECTOR_THRUSTER_MAX_ANGULAR_VELOCITY;
    public static final ModConfigSpec.BooleanValue VECTOR_THRUSTER_PRECISE_OBSTRUCTION_RAYCAST;

    public static final ModConfigSpec.DoubleValue LIQUID_VECTOR_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.IntValue LIQUID_VECTOR_THRUSTER_FUEL_TANK_CAPACITY_MB;
    public static final ModConfigSpec.DoubleValue LIQUID_VECTOR_THRUSTER_FUEL_MB_PER_TICK_AT_FULL_THROTTLE;

    public static final ModConfigSpec.DoubleValue SOLID_FUEL_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue SOLID_FUEL_THRUSTER_NOZZLE_OFFSET;

    public static final ModConfigSpec.DoubleValue SINGLE_RCS_THRUST;
    public static final ModConfigSpec.DoubleValue RCS_THRUST;
    public static final ModConfigSpec.DoubleValue RCS_HYDRAZINE_THRUST_MULTIPLIER;
    public static final ModConfigSpec.IntValue RCS_HYDRAZINE_DRAIN_PER_TICK;

    public static final ModConfigSpec.DoubleValue CREATIVE_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_THRUSTER_MAX_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_THRUSTER_MULTIBLOCK_2X2X2_MAX_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_THRUSTER_MULTIBLOCK_3X3X3_MAX_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_VECTOR_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_VECTOR_THRUSTER_MAX_THRUST;

    // ── Common: machines ────────────────────────────────────────────────────
    public static final ModConfigSpec.IntValue HYDROPUMP_ENERGY_CAPACITY_FE;
    public static final ModConfigSpec.IntValue HYDROPUMP_FE_PER_TICK_AT_MAX_SPEED;
    public static final ModConfigSpec.IntValue HYDROPUMP_MAX_PUMPING_SPEED_MB;

    public static final ModConfigSpec.DoubleValue STIRLING_GENERATED_SU;

    public static final ModConfigSpec.BooleanValue BURNERS_POWER_HEATED_MIXERS;
    public static final ModConfigSpec.BooleanValue BURNERS_HEAT_STEAM_ENGINES;
    public static final ModConfigSpec.BooleanValue BURNERS_SUPERHEAT_STEAM_ENGINES;
    public static final ModConfigSpec.BooleanValue BLAZE_BURNERS_HEAT_STIRLING_ENGINES;
    public static final ModConfigSpec.DoubleValue SOLID_BURNER_FUEL_CONSUMPTION_MULTIPLIER;

    public static final ModConfigSpec.IntValue CABLE_ENERGY_TRANSFER;

    public static final ModConfigSpec.IntValue AUTO_GLUE_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue CONTRAPTION_MOVER_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue CONTRAPTION_CLONER_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue CONTRAPTION_REMOVER_MAX_BLOCKS;

    public static final ModConfigSpec.BooleanValue DISABLE_WORLDGEN;

    public static final Map<String, ModConfigSpec.BooleanValue> ITEM_ENABLED = new LinkedHashMap<>();

    // ── Common: fuels ───────────────────────────────────────────────────────
    public static final Map<String, ModConfigSpec.IntValue> FUEL_EFFICIENCY_ENTRIES = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.IntValue> FUEL_BURN_RATE_ENTRIES = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.ConfigValue<String>> CORAL_FUEL_CONVERSION_RATE_ENTRIES = new LinkedHashMap<>();
    public static final ModConfigSpec.ConfigValue<String> ADDITIONAL_THRUSTER_FUEL_PROPERTY_LINES;

    // ── Client ──────────────────────────────────────────────────────────────
    public static final ModConfigSpec.EnumValue<PlumeRenderMode> PLUME_RENDER_MODE;
    public static final ModConfigSpec.BooleanValue PLUME_VEIL_LIGHT;
    public static final ModConfigSpec.BooleanValue DISABLE_PARTICLE_PHYSICS;
    public static final ModConfigSpec.BooleanValue DISABLE_PARTICLE_PLUME_CURVING;
    public static final ModConfigSpec.IntValue CLIENT_PARTICLES_PER_TICK;
    public static final ModConfigSpec.DoubleValue SOLID_FUEL_THRUSTER_PARTICLE_COUNT_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SOLID_FUEL_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER;
    public static final Map<String, ModConfigSpec.ConfigValue<String>> THRUSTER_DYE_COLORS = new LinkedHashMap<>();

    public static final ModConfigSpec.DoubleValue RCS_FLAME_SCALE;
    public static final ModConfigSpec.DoubleValue RCS_SOUND_VOLUME;
    public static final ModConfigSpec.IntValue RCS_SOUND_RANGE;

    public static final ModConfigSpec.DoubleValue STIRLING_REVOLUTION_PERIOD;
    public static final ModConfigSpec.DoubleValue STIRLING_CRANK_RADIUS;
    public static final ModConfigSpec.DoubleValue STIRLING_CONROD_LENGTH;

    public static final ModConfigSpec.BooleanValue DEBUG_THRUSTER;

    static {
        //#region Common (server)
        COMMON_BUILDER.comment("Settings shared by all thrusters. All thrust values are in pN, the unit shown on goggles tooltips.")
                .push("thrusterGeneral");
        THRUST_UNITS_PER_PN = COMMON_BUILDER.comment("How many internal physics thrust units equal 1 pN of displayed thrust.")
                .defineInRange("thrustUnitsPerPn", 1000.0d, 1.0d, 1000000.0d);
        VECTOR_CORE_OUTPUT_PN = COMMON_BUILDER.comment("OutputPN in the Vector Core durability drain per tick: (thrustPN / OutputPN)^0.7 / 20.")
                .defineInRange("vectorCoreOutputPn", 1000.0d, 1.0d, 1000000000.0d);
        NOZZLE_OFFSET_FROM_CENTER = COMMON_BUILDER.comment("Offset from the block center where force is applied.")
                .defineInRange("nozzleOffsetFromCenter", 0.45d, 0.0d, 1.5d);
        OBSTRUCTION_SCAN_LENGTH = COMMON_BUILDER.comment("How many blocks behind the nozzle are checked for obstruction.")
                .defineInRange("obstructionScanLength", 10, 1, 64);
        OBSTRUCTION_IGNORE_OTHER_SUBLEVELS = COMMON_BUILDER.comment("Ignore non-sublevel blocks when checking for obstruction.")
                .define("obstructionIgnoreOtherSubLevels", true);
        DAMAGE_ENTITIES = COMMON_BUILDER.comment("If true, entities inside an active thruster plume are damaged.")
                .define("damageEntities", true);
        USE_ATMOSPHERIC_PRESSURE = COMMON_BUILDER.comment("If true, atmospheric pressure affects thruster output at altitude.")
                .define("useAtmosphericPressure", false);
        ATMOSPHERIC_PRESSURE_AMOUNT = COMMON_BUILDER.comment("Strength of atmospheric pressure influence. 1.0 = full effect, 0.0 = no effect.")
                .defineInRange("atmosphericPressureAmount", 1.0d, 0.0d, 2.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Standard fuel thruster.").push("thruster");
        BASE_THRUST = COMMON_BUILDER.comment("Thrust (pN) at redstone 15 and no obstruction, before fuel multipliers.")
                .defineInRange("baseThrust", 800.0d, 1.0d, 10000000.0d);
        FUEL_TANK_CAPACITY_MB = COMMON_BUILDER.comment("Internal fuel tank capacity in millibuckets.")
                .defineInRange("fuelTankCapacityMb", 1000, 250, 10000000);
        FUEL_MB_PER_TICK_AT_FULL_THROTTLE = COMMON_BUILDER.comment("Fuel consumption in millibuckets per tick at full throttle.")
                .defineInRange("fuelMbPerTickAtFullThrottle", 1.0d, 0.0001d, 1000.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Bonuses for assembled fuel thruster cubes. Cost multipliers below 1.0 make fuel or oxidizer cheaper.")
                .push("multiblockThruster");
        MULTIBLOCK_2X_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Thrust multiplier for a 2x2x2 cube (1.25 = +25%).")
                .defineInRange("thrustMultiplier2x", 1.25d, 0.01d, 10.0d);
        MULTIBLOCK_3X_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Thrust multiplier for a 3x3x3 cube (1.5 = +50%).")
                .defineInRange("thrustMultiplier3x", 1.5d, 0.01d, 10.0d);
        MULTIBLOCK_2X_FUEL_COST_MULTIPLIER = COMMON_BUILDER.comment("Fuel cost multiplier for a 2x2x2 cube (0.6 = 40% cheaper).")
                .defineInRange("fuelCostMultiplier2x", 0.6d, 0.01d, 10.0d);
        MULTIBLOCK_3X_FUEL_COST_MULTIPLIER = COMMON_BUILDER.comment("Fuel cost multiplier for a 3x3x3 cube (0.4 = 60% cheaper).")
                .defineInRange("fuelCostMultiplier3x", 0.4d, 0.01d, 10.0d);
        MULTIBLOCK_2X_OXIDIZER_COST_MULTIPLIER = COMMON_BUILDER.comment("Oxidizer cost multiplier for a 2x2x2 cube (0.85 = 15% cheaper).")
                .defineInRange("oxidizerCostMultiplier2x", 0.85d, 0.01d, 10.0d);
        MULTIBLOCK_3X_OXIDIZER_COST_MULTIPLIER = COMMON_BUILDER.comment("Oxidizer cost multiplier for a 3x3x3 cube (0.75 = 25% cheaper).")
                .defineInRange("oxidizerCostMultiplier3x", 0.75d, 0.01d, 10.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Ion thruster.").push("ionThruster");
        ION_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Thrust (pN) at redstone 15 and no obstruction.")
                .defineInRange("baseThrust", 1000.0d, 1.0d, 10000000.0d);
        ION_THRUSTER_ENERGY_CAPACITY_FE = COMMON_BUILDER.comment("Internal FE capacity.")
                .defineInRange("energyCapacityFe", 4000, 1, 100000000);
        ION_THRUSTER_FE_PER_TICK_AT_FULL_THROTTLE = COMMON_BUILDER.comment("Energy consumption in FE per tick at full throttle.")
                .defineInRange("fePerTickAtFullThrottle", 40.0d, 0.0001d, 1000000.0d);
        ION_MULTIBLOCK_2X_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Thrust multiplier for a 2x2x2 cube (1.30 = +30%).")
                .defineInRange("thrustMultiplier2x", 1.30d, 0.01d, 10.0d);
        ION_MULTIBLOCK_3X_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Thrust multiplier for a 3x3x3 cube (1.40 = +40%).")
                .defineInRange("thrustMultiplier3x", 1.40d, 0.01d, 10.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Vector thruster (ion powered).").push("vectorThruster");
        VECTOR_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Thrust (pN) at redstone 15 and no obstruction.")
                .defineInRange("baseThrust", 1000.0d, 1.0d, 10000000.0d);
        VECTOR_THRUSTER_MAX_ANGLE = COMMON_BUILDER.comment("Maximum nozzle deflection in degrees.")
                .defineInRange("maxAngleDegrees", 30.0d, 0.0d, 90.0d);
        VECTOR_THRUSTER_MAX_ANGULAR_VELOCITY = COMMON_BUILDER.comment("How fast the nozzle turns, in degrees per second.")
                .defineInRange("maxAngularVelocityDegreesPerSecond", 120.0d, 0.0d, 3600.0d);
        VECTOR_THRUSTER_PRECISE_OBSTRUCTION_RAYCAST = COMMON_BUILDER.comment("Use a more precise but more expensive obstruction check for vector thrusters.")
                .define("preciseObstructionRaycast", false);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Liquid fuel vector thruster.").push("liquidVectorThruster");
        LIQUID_VECTOR_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Thrust (pN) at redstone 15 and no obstruction, before fuel multipliers.")
                .defineInRange("baseThrust", 800.0d, 1.0d, 10000000.0d);
        LIQUID_VECTOR_THRUSTER_FUEL_TANK_CAPACITY_MB = COMMON_BUILDER.comment("Internal fuel tank capacity in millibuckets.")
                .defineInRange("fuelTankCapacityMb", 1000, 250, 10000000);
        LIQUID_VECTOR_THRUSTER_FUEL_MB_PER_TICK_AT_FULL_THROTTLE = COMMON_BUILDER.comment("Fuel consumption in millibuckets per tick at full throttle.")
                .defineInRange("fuelMbPerTickAtFullThrottle", 1.0d, 0.0001d, 1000.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Solid fuel thruster.").push("solidFuelThruster");
        SOLID_FUEL_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Thrust (pN) at redstone 15 and no obstruction, before fuel multipliers.")
                .defineInRange("baseThrust", 400.0d, 1.0d, 10000000.0d);
        SOLID_FUEL_THRUSTER_NOZZLE_OFFSET = COMMON_BUILDER.comment("Offset from the block center where force is applied.")
                .defineInRange("nozzleOffset", 0.75d, 0.0d, 1.5d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("RCS thrusters. Thrust is per nozzle.").push("rcsThruster");
        SINGLE_RCS_THRUST = COMMON_BUILDER.comment("Full thrust (pN) of the single RCS thruster.")
                .defineInRange("singleThrust", 120.0d, 0.0d, 10000000.0d);
        RCS_THRUST = COMMON_BUILDER.comment("Full thrust (pN) of each nozzle on the quad RCS thruster.")
                .defineInRange("fullThrustPerNozzle", 120.0d, 0.0d, 10000000.0d);
        RCS_HYDRAZINE_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Thrust multiplier while the thruster is boosted with hydrazine.")
                .defineInRange("hydrazineThrustMultiplier", 2.0d, 1.0d, 1000.0d);
        RCS_HYDRAZINE_DRAIN_PER_TICK = COMMON_BUILDER.comment("Hydrazine used in millibuckets per tick at full power on all nozzles. Scales with power.")
                .defineInRange("hydrazineDrainPerTick", 2, 0, 1000);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Creative thruster. Thrust values are in pN.").push("creativeThruster");
        CREATIVE_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Starting thrust when a creative thruster is placed.")
                .defineInRange("baseThrust", 1000.0d, 1.0d, 1000000.0d);
        CREATIVE_THRUSTER_MAX_THRUST = COMMON_BUILDER.comment("Maximum thrust the scroll can reach.")
                .defineInRange("maxThrust", 10000.0d, 10.0d, 1000000.0d);
        CREATIVE_THRUSTER_MULTIBLOCK_2X2X2_MAX_THRUST = COMMON_BUILDER.comment("Maximum thrust the scroll can reach on a 2x2x2 cube.")
                .defineInRange("multiblockMaxThrust2x", 100000.0d, 10.0d, 100000000.0d);
        CREATIVE_THRUSTER_MULTIBLOCK_3X3X3_MAX_THRUST = COMMON_BUILDER.comment("Maximum thrust the scroll can reach on a 3x3x3 cube.")
                .defineInRange("multiblockMaxThrust3x", 5000000.0d, 10.0d, 100000000.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Creative vector thruster. Thrust values are in pN.").push("creativeVectorThruster");
        CREATIVE_VECTOR_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Starting thrust when a creative vector thruster is placed.")
                .defineInRange("baseThrust", 1000.0d, 1.0d, 1000000.0d);
        CREATIVE_VECTOR_THRUSTER_MAX_THRUST = COMMON_BUILDER.comment("Maximum thrust the scroll can reach.")
                .defineInRange("maxThrust", 10000.0d, 10.0d, 1000000.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Hydropump.").push("hydropump");
        HYDROPUMP_ENERGY_CAPACITY_FE = COMMON_BUILDER.comment("Internal FE capacity.")
                .defineInRange("energyCapacityFe", 1000, 1, 100000000);
        HYDROPUMP_FE_PER_TICK_AT_MAX_SPEED = COMMON_BUILDER.comment("Energy consumption in FE per tick at maximum pumping speed.")
                .defineInRange("fePerTickAtMaxSpeed", 20, 1, 1000000);
        HYDROPUMP_MAX_PUMPING_SPEED_MB = COMMON_BUILDER.comment("Maximum pumping speed in millibuckets per 20 ticks.")
                .defineInRange("maxPumpingSpeedMbPer20Ticks", 2000, 2, 1000000);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Stirling engine.").push("stirlingEngine");
        STIRLING_GENERATED_SU = COMMON_BUILDER.comment("Stress units produced by the stirling engine. A value of 16 corresponds to 4096 SU.")
                .defineInRange("generatedStressUnits", 16.0d, 1.0d, 64.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Burners.").push("burners");
        BURNERS_POWER_HEATED_MIXERS = COMMON_BUILDER.comment("If true, solid and liquid burners can heat mixers, allowing pre-nether brass.")
                .define("powerHeatedMixers", true);
        BURNERS_HEAT_STEAM_ENGINES = COMMON_BUILDER.comment("Allow propulsion burners to heat Create steam engines.")
                .define("heatSteamEngines", true);
        BURNERS_SUPERHEAT_STEAM_ENGINES = COMMON_BUILDER.comment("Allow seething burners to count as superheated for steam engines.")
                .define("superheatSteamEngines", true);
        BLAZE_BURNERS_HEAT_STIRLING_ENGINES = COMMON_BUILDER.comment("Allow vanilla blaze burners under stirling engines to provide heat.")
                .define("blazeBurnersHeatStirlingEngines", true);
        SOLID_BURNER_FUEL_CONSUMPTION_MULTIPLIER = COMMON_BUILDER.comment("Fuel consumption multiplier for solid burners. Higher values make inserted items burn faster.")
                .defineInRange("solidBurnerFuelConsumptionMultiplier", 1.0d, 0.01d, 100.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Energy cables.").push("cable");
        CABLE_ENERGY_TRANSFER = COMMON_BUILDER.comment("Maximum total FE moved per tick by each connected cable network, shared by all sources and consumers. Adding cables does not increase this limit.")
                .defineInRange("energyTransferPerTick", 1_000, 1, 100000000);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Creative tools.").push("creativeTools");
        AUTO_GLUE_MAX_BLOCKS = COMMON_BUILDER.comment(
                        "Maximum connected blocks Auto Glue may scan before aborting.",
                        "Higher values can increase server load when processing very large structures.")
                .defineInRange("autoGlueMaxBlocks", 8_192, 1, Integer.MAX_VALUE);
        CONTRAPTION_MOVER_MAX_BLOCKS = COMMON_BUILDER.comment(
                        "Maximum connected blocks the Contraption Mover may capture.",
                        "Higher values increase schematic size and server processing cost.")
                .defineInRange("contraptionMoverMaxBlocks", 16_384, 1, Integer.MAX_VALUE);
        CONTRAPTION_CLONER_MAX_BLOCKS = COMMON_BUILDER.comment(
                        "Maximum connected blocks the Contraption Cloner may capture.",
                        "Higher values increase schematic size and server processing cost.")
                .defineInRange("contraptionClonerMaxBlocks", 16_384, 1, Integer.MAX_VALUE);
        CONTRAPTION_REMOVER_MAX_BLOCKS = COMMON_BUILDER.comment(
                        "Maximum connected blocks the Contraption Remover may delete.",
                        "Higher values can increase server load when removing very large structures.")
                .defineInRange("contraptionRemoverMaxBlocks", 16_384, 1, Integer.MAX_VALUE);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Worldgen.").push("worldgen");
        DISABLE_WORLDGEN = COMMON_BUILDER.comment("If true, platinum ore does not generate. Applies to newly generated chunks after a world restart.")
                .define("disableWorldgen", false);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment(
                        "Item toggles. Set an item to false to hide it from the creative tab and disable every recipe that produces it.",
                        "Recipe changes apply on world load or /reload.")
                .push("items");
        for (String itemPath : PropulsionItemToggles.collectItemPaths()) {
            ITEM_ENABLED.put(itemPath, COMMON_BUILDER.define(itemPath, true));
        }
        COMMON_BUILDER.pop();

        COMMON_BUILDER.comment("Fuel tables.").push("fuels");
        COMMON_BUILDER.comment("Thruster fuel properties by fluid id. Efficiency and burn rate are percentages.");
        COMMON_BUILDER.push("fuelProperties");
        for (String entry : defaultFuelProperties()) {
            String[] split = entry.split("=", 2);
            if (split.length != 2) continue;
            String fluidId = split[0];
            String[] values = split[1].split(",", 2);
            if (values.length != 2) continue;
            int efficiency;
            int burnRate;
            try {
                efficiency = Integer.parseInt(values[0].trim());
                burnRate = Integer.parseInt(values[1].trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            COMMON_BUILDER.push(configKeyForFluidId(fluidId));
            FUEL_EFFICIENCY_ENTRIES.put(fluidId,
                    COMMON_BUILDER.comment("Fuel efficiency percentage for " + fluidId + ".")
                            .defineInRange("efficiency", efficiency, 0, 10000));
            FUEL_BURN_RATE_ENTRIES.put(fluidId,
                    COMMON_BUILDER.comment("Fuel burn rate percentage for " + fluidId + ".")
                            .defineInRange("burnRate", burnRate, 0, 10000));
            COMMON_BUILDER.pop();
        }
        COMMON_BUILDER.pop();

        ADDITIONAL_THRUSTER_FUEL_PROPERTY_LINES = COMMON_BUILDER.comment(
                        "Additional thruster fuels separated by ';' (fluid_id=efficiencyPercent,burnRatePercent), for example: mod:fuel=100,100;mod:other=80,120.",
                        "Use for fluids that do not have a fuelProperties subsection. Entries here override matching fluids from the table above.")
                .define("additionalThrusterFuelLines", "");

        COMMON_BUILDER.comment(
                "Coral generator fuels. Each key is a fluid id and each value is the FE produced per millibucket.");
        COMMON_BUILDER.push("coralFuelConversionRates");
        for (String entry : defaultCoralFuelConversionRates()) {
            String[] split = entry.split("=", 2);
            if (split.length != 2) continue;
            CORAL_FUEL_CONVERSION_RATE_ENTRIES.put(split[0], COMMON_BUILDER.define(configKeyForFluidId(split[0]), split[1]));
        }
        COMMON_BUILDER.pop();
        COMMON_BUILDER.pop();

        PropulsionDefaultStress.INSTANCE.registerAll(COMMON_BUILDER);

        COMMON_SPEC = COMMON_BUILDER.build();
        //#endregion

        //#region Client
        CLIENT_BUILDER.comment("Thruster plume rendering.").push("thrusterPlumes");
        PLUME_RENDER_MODE = CLIENT_BUILDER.comment("How every thruster plume is rendered. Particle uses normal particle effects; Shader uses the procedural plume shader; Shader 2.0 uses the blocky voxel plume with a dynamic light.")
                .defineEnum("renderMode", PlumeRenderMode.PARTICLE);
        PLUME_VEIL_LIGHT = CLIENT_BUILDER.comment("Shader 2.0 only: thrusters cast a dynamic Veil light.")
                .define("veilLight", true);
        DISABLE_PARTICLE_PHYSICS = CLIENT_BUILDER.comment("Particle mode only: disable plume particle collisions.")
                .define("disableParticlePhysics", false);
        DISABLE_PARTICLE_PLUME_CURVING = CLIENT_BUILDER.comment("Particle mode only: disable plume particles curving with the thruster's motion.")
                .define("disableParticlePlumeCurving", false);
        CLIENT_PARTICLES_PER_TICK = CLIENT_BUILDER.comment("Particle mode only: maximum particles per tick per active thruster.")
                .defineInRange("particlesPerTick", 4, 0, 64);
        SOLID_FUEL_THRUSTER_PARTICLE_COUNT_MULTIPLIER = CLIENT_BUILDER.comment("Particle mode only: scales exhaust particle count for solid fuel thrusters.")
                .defineInRange("solidFuelParticleCountMultiplier", 0.35d, 0.0d, 32.0d);
        SOLID_FUEL_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER = CLIENT_BUILDER.comment("Particle mode only: scales exhaust particle velocity for solid fuel thrusters.")
                .defineInRange("solidFuelParticleVelocityMultiplier", 0.4d, 0.0d, 32.0d);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.comment("Plume color overrides when a dye is applied to a thruster. Values are RRGGBB hex strings.")
                .push("thrusterDyeColors");
        for (String[] e : new String[][]{
                {"white", "FFFFFF"},
                {"orange", "FF8000"},
                {"magenta", "FF00FF"},
                {"light_blue", "00BFFF"},
                {"yellow", "FFFF00"},
                {"lime", "7FFF00"},
                {"pink", "FF69B4"},
                {"gray", "808080"},
                {"light_gray", "C0C0C0"},
                {"cyan", "00FFFF"},
                {"purple", "BF00FF"},
                {"blue", "5555FF"},
                {"brown", "C86400"},
                {"green", "00C800"},
                {"red", "FF0000"},
                {"black", "2A2A2A"},
        }) {
            THRUSTER_DYE_COLORS.put("minecraft:" + e[0] + "_dye",
                    CLIENT_BUILDER.define(e[0], e[1]));
        }
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.comment("RCS thruster visuals and sound.").push("rcsThruster");
        RCS_FLAME_SCALE = CLIENT_BUILDER.comment("Scale of the RCS flame.")
                .defineInRange("flameScale", 1.0d, 0.0d, 8.0d);
        RCS_SOUND_VOLUME = CLIENT_BUILDER.comment("Volume of the RCS sound.")
                .defineInRange("soundVolume", 1.0d, 0.0d, 4.0d);
        RCS_SOUND_RANGE = CLIENT_BUILDER.comment("Distance in blocks at which the RCS sound fades out.")
                .defineInRange("soundRangeBlocks", 32, 1, 256);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.comment("Stirling engine animation. Affects only the look of the pistons.").push("stirlingEngine");
        STIRLING_REVOLUTION_PERIOD = CLIENT_BUILDER.comment("Revolution period of the simulated shaft.")
                .defineInRange("revolutionPeriod", 0.2d, 0.001d, 100.0d);
        STIRLING_CRANK_RADIUS = CLIENT_BUILDER.comment("Radius of the simulated crank.")
                .defineInRange("crankRadius", 0.125d, 0.001d, 10.0d);
        STIRLING_CONROD_LENGTH = CLIENT_BUILDER.comment("Length of the simulated conrod.")
                .defineInRange("conrodLength", 0.5d, 0.001d, 10.0d);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.comment("Debug overlays.").push("debug");
        DEBUG_THRUSTER = CLIENT_BUILDER.comment("Render thruster debug overlays (plume ray, obstruction hits, damage zones).")
                .define("thruster", false);
        CLIENT_BUILDER.pop();

        CLIENT_SPEC = CLIENT_BUILDER.build();
        //#endregion
    }

    private static List<String> defaultFuelProperties() {
        return new ArrayList<>(List.of(
                "createpropulsion:turpentine=100,150",

                "minecraft:lava=75,100",

                "ad_astra:cryo_fuel=230,60",
                "ad_astra:fuel=115,100",
                "ad_astra:hydrogen=230,90",

                "createaddition:bioethanol=75,135",
                "createaddition:seed_oil=55,170",

                "createdieselgenerators:biodiesel=90,110",
                "createdieselgenerators:diesel=100,100",
                "createdieselgenerators:ethanol=70,140",
                "createdieselgenerators:gasoline=125,80",
                "createdieselgenerators:plant_oil=55,170",

                "gtceu:benzene=110,100",
                "gtceu:bio_diesel=90,110",
                "gtceu:butadiene=110,100",
                "gtceu:butane=125,80",
                "gtceu:butene=115,90",
                "gtceu:cetane_boosted_diesel=150,75",
                "gtceu:coal_gas=70,145",
                "gtceu:diesel=100,100",
                "gtceu:ethane=110,95",
                "gtceu:ethanol=70,140",
                "gtceu:ethylene=105,100",
                "gtceu:gasoline=125,80",
                "gtceu:high_octane_gasoline=175,60",
                "gtceu:light_fuel=100,105",
                "gtceu:lpg=120,85",
                "gtceu:methane=105,95",
                "gtceu:methanol=60,155",
                "gtceu:naphtha=95,105",
                "gtceu:natural_gas=100,105",
                "gtceu:nitrobenzene=150,80",
                "gtceu:octane=125,85",
                "gtceu:phenol=85,125",
                "gtceu:propane=115,90",
                "gtceu:propene=110,95",
                "gtceu:refinery_gas=105,100",
                "gtceu:rocket_fuel=230,85",
                "gtceu:sulfuric_gas=80,135",
                "gtceu:sulfuric_light_fuel=80,135",
                "gtceu:sulfuric_naphtha=75,140",
                "gtceu:toluene=105,105",
                "gtceu:uu_matter=1000,25",
                "gtceu:wood_gas=60,160",

                "hbm:balefire=750,5",
                "hbm:biofuel=90,110",
                "hbm:biogas=65,165",
                "hbm:coalgas=110,100",
                "hbm:coalgas_leaded=125,85",
                "hbm:deuterium=230,90",
                "hbm:diesel=100,100",
                "hbm:diesel_crack=95,110",
                "hbm:diesel_crack_reform=140,80",
                "hbm:diesel_reform=150,75",
                "hbm:ethanol=70,140",
                "hbm:gas=80,125",
                "hbm:gas_coker=85,115",
                "hbm:gasoline=125,80",
                "hbm:gasoline_leaded=150,65",
                "hbm:heatingoil=75,145",
                "hbm:heatingoil_vacuum=90,120",
                "hbm:heavyoil=60,170",
                "hbm:heavyoil_vacuum=75,140",
                "hbm:hydrogen=230,90",
                "hbm:kerosene=230,90",
                "hbm:kerosene_reform=280,60",
                "hbm:lightoil=100,105",
                "hbm:lightoil_crack=95,115",
                "hbm:lightoil_ds=115,90",
                "hbm:lightoil_vacuum=110,95",
                "hbm:lpg=120,85",
                "hbm:naphtha=95,105",
                "hbm:naphtha_coker=100,100",
                "hbm:naphtha_crack=90,115",
                "hbm:naphtha_ds=110,90",
                "hbm:nitan=500,15",
                "hbm:oxyhydrogen=180,110",
                "hbm:petroil=80,135",
                "hbm:petroil_leaded=95,110",
                "hbm:petroleum=110,95",
                "hbm:reclaimed=70,155",
                "hbm:reformate=150,70",
                "hbm:reformgas=150,65",
                "hbm:smear=60,175",
                "hbm:syngas=100,110",
                "hbm:tritium=230,90",
                "hbm:xylene=140,75",

                "immersiveengineering:biodiesel=90,110",
                "immersiveengineering:creosote=50,180",
                "immersiveengineering:ethanol=70,140",
                "immersiveengineering:high_power_biodiesel=105,95",
                "immersiveengineering:plantoil=55,170",

                "immersivepetroleum:diesel_sulfur=100,100",

                "mekanism:hydrogen=230,90",

                "mekanismdieselgenerators:biodiesel=90,110",
                "mekanismdieselgenerators:diesel=100,100",
                "mekanismdieselgenerators:gasoline=125,80",
                "mekanismdieselgenerators:heavy_fuel=85,125",
                "mekanismdieselgenerators:hydrogen=230,90",
                "mekanismdieselgenerators:kerosene=230,90",
                "mekanismdieselgenerators:methane=105,95",
                "mekanismdieselgenerators:naphtha=95,105",

                "mekanismgenerators:bioethanol=75,135",

                "modern_industrialization:biodiesel=90,110",
                "modern_industrialization:boosted_diesel=150,75",
                "modern_industrialization:creosote=50,180",
                "modern_industrialization:diesel=100,100",
                "modern_industrialization:heavy_fuel=85,125",
                "modern_industrialization:hydrogen=230,90",
                "modern_industrialization:light_fuel=100,105",
                "modern_industrialization:naphtha=95,105",
                "modern_industrialization:raw_biodiesel=70,145",
                "modern_industrialization:synthetic_oil=75,140",
                "modern_industrialization:uu_matter=1000,25",

                "northstar:biofuel=80,125",
                "northstar:hydrocarbon=130,75",
                "northstar:hydrogen=230,90",
                "northstar:liquid_hydrogen=230,90",
                "northstar:methane=105,95",

                "petrochem:diesel=100,100",
                "petrochem:fuel_oil=85,125",
                "petrochem:gasoline=125,80",
                "petrochem:hydrogen=230,90",
                "petrochem:kerosene=230,90",
                "petrochem:lpg=120,85",

                "stellaris:diesel=100,100",
                "stellaris:fuel=115,100",
                "stellaris:hydrogen=230,90",

                "tfmg:butane=125,80",
                "tfmg:creosote=50,180",
                "tfmg:diesel=100,100",
                "tfmg:furnace_gas=45,190",
                "tfmg:gasoline=125,80",
                "tfmg:hydrogen=230,90",
                "tfmg:kerosene=230,90",
                "tfmg:lpg=120,85",
                "tfmg:naphtha=95,105",
                "tfmg:propane=115,90"
        ));
    }

    private static List<String> defaultCoralFuelConversionRates() {
        return new ArrayList<>(List.of("createpropulsion:coral=500"));
    }

    private static String configKeyForFluidId(String fluidId) {
        return fluidId
                .replace(':', '_')
                .replace('/', '_')
                .replace('.', '_')
                .replace('-', '_');
    }


    public static boolean isItemEnabled(ResourceLocation itemId) {
        if (itemId == null || !CreatePropulsion.ID.equals(itemId.getNamespace()) || !COMMON_SPEC.isLoaded()) {
            return true;
        }
        ModConfigSpec.BooleanValue value = ITEM_ENABLED.get(itemId.getPath());
        return value == null || value.get();
    }

    public static Integer getDyeColor(String dyeId) {
        ModConfigSpec.ConfigValue<String> cv = THRUSTER_DYE_COLORS.get(dyeId);
        if (cv == null) return null;
        try {
            return Integer.parseUnsignedInt(cv.get().trim(), 16);
        } catch (NumberFormatException | IllegalStateException ignored) {
            return null;
        }
    }

    public static boolean isDyeConfigured(String itemId) {
        return THRUSTER_DYE_COLORS.containsKey(itemId);
    }

    public static List<? extends String> getCoralFuelConversionRatesOrDefault() {
        if (!CORAL_FUEL_CONVERSION_RATE_ENTRIES.isEmpty()) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, ModConfigSpec.ConfigValue<String>> e : CORAL_FUEL_CONVERSION_RATE_ENTRIES.entrySet()) {
                try {
                    entries.add(e.getKey() + "=" + e.getValue().get());
                } catch (IllegalStateException ignored) {
                    return defaultCoralFuelConversionRates();
                }
            }
            return entries;
        }
        return defaultCoralFuelConversionRates();
    }

    public static List<? extends String> getFuelPropertiesOrDefault() {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>();

        if (!FUEL_EFFICIENCY_ENTRIES.isEmpty() && !FUEL_BURN_RATE_ENTRIES.isEmpty()) {
            for (Map.Entry<String, ModConfigSpec.IntValue> e : FUEL_EFFICIENCY_ENTRIES.entrySet()) {
                try {
                    ModConfigSpec.IntValue burnRate = FUEL_BURN_RATE_ENTRIES.get(e.getKey());
                    if (burnRate == null) {
                        continue;
                    }
                    merged.put(e.getKey(), e.getValue().get() + "," + burnRate.get());
                } catch (IllegalStateException ignored) {
                    // Config not ready — skip entry
                }
            }
        }

        try {
            for (String raw : ADDITIONAL_THRUSTER_FUEL_PROPERTY_LINES.get().split(";")) {
                String line = raw.trim();
                int sep = line.indexOf('=');
                if (sep <= 0 || sep >= line.length() - 1) {
                    continue;
                }
                String fluidId = line.substring(0, sep).trim();
                String rhs = line.substring(sep + 1).trim();
                if (ResourceLocation.tryParse(fluidId) == null || !rhs.contains(",")) {
                    continue;
                }
                merged.put(fluidId, rhs);
            }
        } catch (IllegalStateException ignored) {
            // ignore until config load completes
        }

        if (merged.isEmpty()) {
            return defaultFuelProperties();
        }

        List<String> out = new ArrayList<>(merged.size());
        for (Map.Entry<String, String> e : merged.entrySet()) {
            out.add(e.getKey() + "=" + e.getValue());
        }
        return out;
    }

    public static double getLiquidVectorThrusterBaseThrustOrDefault() {
        return orDefault(LIQUID_VECTOR_THRUSTER_BASE_THRUST);
    }

    public static int getLiquidVectorThrusterFuelTankCapacityMbOrDefault() {
        return orDefault(LIQUID_VECTOR_THRUSTER_FUEL_TANK_CAPACITY_MB);
    }

    public static double getLiquidVectorThrusterFuelMbPerTickAtFullThrottleOrDefault() {
        return orDefault(LIQUID_VECTOR_THRUSTER_FUEL_MB_PER_TICK_AT_FULL_THROTTLE);
    }

    public static double getThrustUnitsPerPnOrDefault() {
        return orDefault(THRUST_UNITS_PER_PN);
    }

    private static <T> T orDefault(ModConfigSpec.ConfigValue<T> value) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return value.getDefault();
        }
    }

    public static boolean useVeilLight() {
        try {
            return useShader2Plumes() && PLUME_VEIL_LIGHT.get();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public static boolean useShader2Plumes() {
        try {
            return PLUME_RENDER_MODE.get() == PlumeRenderMode.SHADER_2;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public static boolean useShaderPlumes() {
        try {
            return PLUME_RENDER_MODE.get() != PlumeRenderMode.PARTICLE;
        } catch (IllegalStateException ignored) {
            // Client configs are not loaded on dedicated servers. Particle plumes are the
            // safe fallback because server-side thruster ticking uses this value too.
            return false;
        }
    }

}
