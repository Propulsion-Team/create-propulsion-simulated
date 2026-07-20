package dev.propulsionteam.propulsionsimulated;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionDefaultStress;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PropulsionConfig {
    public static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // ── Thruster (reference-style typed values for new code) ────────────────
    public static final ModConfigSpec.DoubleValue BASE_THRUST;
    public static final ModConfigSpec.IntValue OBSTRUCTION_SCAN_LENGTH;
    public static final ModConfigSpec.BooleanValue OBSTRUCTION_IGNORE_OTHER_SUBLEVELS;
    public static final ModConfigSpec.IntValue FUEL_TANK_CAPACITY_MB;

    public static final ModConfigSpec.DoubleValue CREATIVE_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_THRUSTER_MAX_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_THRUSTER_MULTIBLOCK_2X2X2_MAX_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_THRUSTER_MULTIBLOCK_3X3X3_MAX_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_VECTOR_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue CREATIVE_VECTOR_THRUSTER_MAX_THRUST;
    public static final ModConfigSpec.DoubleValue FUEL_MB_PER_TICK_AT_FULL_THROTTLE;

    public static final ModConfigSpec.IntValue ION_THRUSTER_ENERGY_CAPACITY_FE;
    public static final ModConfigSpec.DoubleValue ION_THRUSTER_FE_PER_TICK_AT_FULL_THROTTLE;
    public static final ModConfigSpec.DoubleValue ION_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue ION_MULTIBLOCK_2X_THRUST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ION_MULTIBLOCK_3X_THRUST_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue VECTOR_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue LIQUID_VECTOR_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.IntValue LIQUID_VECTOR_THRUSTER_FUEL_TANK_CAPACITY_MB;
    public static final ModConfigSpec.DoubleValue LIQUID_VECTOR_THRUSTER_FUEL_MB_PER_TICK_AT_FULL_THROTTLE;

    public static final ModConfigSpec.DoubleValue SOLID_FUEL_THRUSTER_BASE_THRUST;
    public static final ModConfigSpec.DoubleValue SOLID_FUEL_THRUSTER_NOZZLE_OFFSET;
    public static final ModConfigSpec.DoubleValue SOLID_FUEL_THRUSTER_PARTICLE_COUNT_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SOLID_FUEL_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER;

    public static final ModConfigSpec.DoubleValue MULTIBLOCK_2X_THRUST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_3X_THRUST_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_2X_FUEL_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_3X_FUEL_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_2X_OXIDIZER_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue MULTIBLOCK_3X_OXIDIZER_EFFICIENCY;
    public static final ModConfigSpec.BooleanValue DAMAGE_ENTITIES;
    public static final ModConfigSpec.DoubleValue NOZZLE_OFFSET_FROM_CENTER;
    public static final ModConfigSpec.BooleanValue USE_ATMOSPHERIC_PRESSURE;
    public static final ModConfigSpec.DoubleValue ATMOSPHERIC_PRESSURE_AMOUNT;
    public static final ModConfigSpec.DoubleValue THRUST_UNITS_PER_KN;
    public static final ModConfigSpec.IntValue CLIENT_PARTICLES_PER_TICK;
    public static final Map<String, ModConfigSpec.IntValue> FUEL_EFFICIENCY_ENTRIES = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.IntValue> FUEL_BURN_RATE_ENTRIES = new LinkedHashMap<>();
    public static final Map<String, ModConfigSpec.ConfigValue<String>> THRUSTER_DYE_COLORS = new LinkedHashMap<>();
    public static final ModConfigSpec.IntValue CABLE_ENERGY_TRANSFER;

    public enum PlumeRenderMode {
        PARTICLE,
        SHADER
    }

    public static final ModConfigSpec.EnumValue<PlumeRenderMode> PLUME_RENDER_MODE;


    public static final ModConfigSpec.BooleanValue DEBUG_THRUSTER;

    // Stirling engine
    public static final ModConfigSpec.ConfigValue<Double> STIRLING_GENERATED_SU;
    public static final ModConfigSpec.ConfigValue<Double> TILT_ADAPTER_ANGLE_RANGE;
    public static final ModConfigSpec.IntValue ADVANCED_TILT_ADAPTER_MAX_ANGLE;
    public static final ModConfigSpec.ConfigValue<Double> STIRLING_REVOLUTION_PERIOD;
    public static final ModConfigSpec.ConfigValue<Double> STIRLING_CRANK_RADIUS;
    public static final ModConfigSpec.ConfigValue<Double> STIRLING_CONROD_LENGTH;

    // Burners
    public static final ModConfigSpec.ConfigValue<Boolean> BURNERS_POWER_HEATED_MIXERS;
    public static final ModConfigSpec.ConfigValue<Boolean> BURNERS_HEAT_STEAM_ENGINES;
    public static final ModConfigSpec.ConfigValue<Boolean> BURNERS_SUPERHEAT_STEAM_ENGINES;
    public static final ModConfigSpec.ConfigValue<Boolean> BLAZE_BURNERS_HEAT_STIRLING_ENGINES;
    public static final ModConfigSpec.ConfigValue<Double> SOLID_BURNER_FUEL_CONSUMPTION_MULTIPLIER;
    public static final Map<String, ModConfigSpec.ConfigValue<String>> CORAL_FUEL_CONVERSION_RATE_ENTRIES = new LinkedHashMap<>();

    /**
     * Extra fuel lines {@code fluid=efficiency,burnRate}; merged after defaults; duplicates override.
     */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADDITIONAL_THRUSTER_FUEL_PROPERTY_LINES;

    static {
        //#region Common (server)
        COMMON_BUILDER.push("thruster");

        BASE_THRUST = COMMON_BUILDER.comment("Base thrust at redstone 15 and full obstruction efficiency for the standard thruster.",
                        "Default tuned for 1000-unit thrust scale parity with Sable physics.",
                        "Effective thrust uses: baseThrust * fuel_thrust_percent / 100.")
                .defineInRange("baseThrust", 533.333333333d, 1.0d, 10000000.0d);
        OBSTRUCTION_SCAN_LENGTH = COMMON_BUILDER.comment("How many blocks behind the nozzle are checked for obstruction.")
                .defineInRange("obstructionScanLength", 10, 1, 64);
        OBSTRUCTION_IGNORE_OTHER_SUBLEVELS = COMMON_BUILDER.comment(
                        "Ignore non-sublevel blocks when checking for obstruction.")
                .define("obstructionIgnoreOtherSubLevels", true);
        FUEL_TANK_CAPACITY_MB = COMMON_BUILDER.comment("Internal fuel tank capacity in millibuckets.")
                .defineInRange("fuelTankCapacityMb", 1000, 250, 10000000);
        FUEL_MB_PER_TICK_AT_FULL_THROTTLE = COMMON_BUILDER.comment("Fuel consumption in millibuckets per tick at full redstone throttle.")
                .defineInRange("fuelMbPerTickAtFullThrottle", 1.0d, 0.0001d, 1000.0d);
        DAMAGE_ENTITIES = COMMON_BUILDER.comment("If true, entities inside active thruster plume are damaged.")
                .define("damageEntities", true);

        CLIENT_PARTICLES_PER_TICK = COMMON_BUILDER.comment("Max client particles per tick while active.")
                .defineInRange("clientParticlesPerTick", 4, 0, 64);

        COMMON_BUILDER.pop(); // thruster

        COMMON_BUILDER.push("ionThruster");
        ION_THRUSTER_ENERGY_CAPACITY_FE = COMMON_BUILDER.comment("Ion thruster internal FE capacity.")
                .defineInRange("ionThrusterEnergyCapacityFe", 4000, 1, 100000000);
        ION_THRUSTER_FE_PER_TICK_AT_FULL_THROTTLE = COMMON_BUILDER.comment("Ion thruster energy consumption in FE per tick at full redstone throttle.")
                .defineInRange("ionThrusterFePerTickAtFullThrottle", 40.0d, 0.0001d, 1000000.0d);
        ION_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Ion thruster base thrust at redstone 15 and full obstruction efficiency.",
                        "Default tuned for 1000-unit thrust scale parity with Sable physics.")
                .defineInRange("ionThrusterBaseThrust", 800.d, 1.d, 10000000.d);
        ION_MULTIBLOCK_2X_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Ion thruster thrust multiplier for 2x2x2 multiblock (1.30 = +30%).")
                .defineInRange("ionMultiblock2xThrustMultiplier", 1.30d, 0.01d, 10.0d);
        ION_MULTIBLOCK_3X_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Ion thruster thrust multiplier for 3x3x3 multiblock (1.40 = +40%).")
                .defineInRange("ionMultiblock3xThrustMultiplier", 1.40d, 0.01d, 10.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("Creative Thruster");
        CREATIVE_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Starting thrust value (kN) when a creative thruster is placed.",
                        "Default tuned for 1000-unit thrust scale parity with Sable physics.")
                .defineInRange("creativeThrusterBaseThrust", 666.666666667d, 1.0d, 1000000.0d);
        CREATIVE_THRUSTER_MAX_THRUST = COMMON_BUILDER.comment("Maximum thrust (kN) the scroll can reach on a creative thruster.",
                        "Default tuned for 1000-unit thrust scale parity with Sable physics.")
                .defineInRange("creativeThrusterMaxThrust", 10000.0d, 10.0d, 1000000.0d);
        CREATIVE_THRUSTER_MULTIBLOCK_2X2X2_MAX_THRUST = COMMON_BUILDER.comment("Maximum thrust (kN) the scroll can reach on a 2x2x2 creative thruster multiblock.")
                .defineInRange("creativeThrusterMultiblock2x2x2MaxThrust", 100000.0d, 10.0d, 100000000.0d);
        CREATIVE_THRUSTER_MULTIBLOCK_3X3X3_MAX_THRUST = COMMON_BUILDER.comment("Maximum thrust (kN) the scroll can reach on a 3x3x3 creative thruster multiblock.")
                .defineInRange("creativeThrusterMultiblock3x3x3MaxThrust", 5000000.0d, 10.0d, 100000000.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("vectorThruster");
        VECTOR_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Vector thruster base thrust at redstone 15 and full obstruction efficiency.",
                        "Default tuned for 1000-unit thrust scale parity with Sable physics.")
                .defineInRange("vectorThrusterBaseThrust", 733.333333333d, 1.0d, 10000000.0d);
        COMMON_BUILDER.pop();
        COMMON_BUILDER.push("liquidVectorThruster");
        LIQUID_VECTOR_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Liquid vector thruster base thrust at redstone 15 and full obstruction efficiency.",
                        "Default tuned for 1000-unit thrust scale parity with Sable physics.")
                .defineInRange("liquidVectorThrusterBaseThrust", 733.333333333d, 1.0d, 10000000.0d);
        LIQUID_VECTOR_THRUSTER_FUEL_TANK_CAPACITY_MB = COMMON_BUILDER.comment("Liquid vector thruster internal fuel tank capacity in millibuckets.")
                .defineInRange("liquidVectorThrusterFuelTankCapacityMb", 1000, 250, 10000000);
        LIQUID_VECTOR_THRUSTER_FUEL_MB_PER_TICK_AT_FULL_THROTTLE = COMMON_BUILDER.comment("Liquid vector thruster fuel consumption in millibuckets per tick at full redstone throttle.")
                .defineInRange("liquidVectorThrusterFuelMbPerTickAtFullThrottle", 1.0d, 0.0001d, 1000.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("solidFuelThruster");
        SOLID_FUEL_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment(
                        "Solid fuel thruster base thrust at redstone 15 and full obstruction efficiency.",
                        "Defaults lower than liquid thrusters for a compact solid-fuel engine.")
                .defineInRange("solidFuelThrusterBaseThrust", 200.0d, 1.0d, 10000000.0d);
        SOLID_FUEL_THRUSTER_NOZZLE_OFFSET = COMMON_BUILDER.comment("Nozzle offset from block center for solid fuel thruster force application.")
                .defineInRange("solidFuelThrusterNozzleOffset", 0.75d, 0.0d, 1.5d);
        SOLID_FUEL_THRUSTER_PARTICLE_COUNT_MULTIPLIER = COMMON_BUILDER.comment(
                        "Scales exhaust particle count for solid fuel thrusters (0 disables extra scaling below base emit logic).")
                .defineInRange("solidFuelThrusterParticleCountMultiplier", 0.35d, 0.0d, 32.0d);
        SOLID_FUEL_THRUSTER_PARTICLE_VELOCITY_MULTIPLIER = COMMON_BUILDER.comment(
                        "Scales exhaust particle velocity for solid fuel thrusters.")
                .defineInRange("solidFuelThrusterParticleVelocityMultiplier", 0.4d, 0.0d, 32.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("multiblockThruster");
        MULTIBLOCK_2X_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Thrust multiplier for a 2x2x2 multiblock thruster (e.g. 1.10 = 10% bonus).")
                .defineInRange("multiblock2xThrustMultiplier", 1.25d, 0.01d, 10.0d);
        MULTIBLOCK_3X_THRUST_MULTIPLIER = COMMON_BUILDER.comment("Thrust multiplier for a 3x3x3 multiblock thruster (e.g. 1.25 = 25% bonus).")
                .defineInRange("multiblock3xThrustMultiplier", 1.5d, 0.01d, 10.0d);
        MULTIBLOCK_2X_FUEL_EFFICIENCY = COMMON_BUILDER.comment("Fuel cost multiplier for a 2x2x2 multiblock thruster (e.g. 1.0 = no reduction, 0.8 = 20% cheaper).")
                .defineInRange("multiblock2xFuelEfficiency", 0.6d, 0.01d, 10.0d);
        MULTIBLOCK_3X_FUEL_EFFICIENCY = COMMON_BUILDER.comment("Fuel cost multiplier for a 3x3x3 multiblock thruster (e.g. 0.95 = 5% cheaper).")
                .defineInRange("multiblock3xFuelEfficiency", 0.4d, 0.01d, 10.0d);
        MULTIBLOCK_2X_OXIDIZER_EFFICIENCY = COMMON_BUILDER.comment("Oxidizer cost multiplier for a 2x2x2 multiblock thruster. 0.85 = 15% savings.")
                .defineInRange("multiblock2xOxidizerEfficiency", 0.85d, 0.01d, 10.0d);
        MULTIBLOCK_3X_OXIDIZER_EFFICIENCY = COMMON_BUILDER.comment("Oxidizer cost multiplier for a 3x3x3 multiblock thruster. 0.75 = 25% savings.")
                .defineInRange("multiblock3xOxidizerEfficiency", 0.75d, 0.01d, 10.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("creativeVectorThruster");
        CREATIVE_VECTOR_THRUSTER_BASE_THRUST = COMMON_BUILDER.comment("Starting thrust value (kN) when a creative vector thruster is placed.",
                        "Default tuned for 1000-unit thrust scale parity with Sable physics.")
                .defineInRange("creativeVectorThrusterBaseThrust", 666.666666667d, 1.0d, 1000000.0d);
        CREATIVE_VECTOR_THRUSTER_MAX_THRUST = COMMON_BUILDER.comment("Maximum thrust (kN) the scroll can reach on a creative vector thruster.",
                        "Default tuned for 1000-unit thrust scale parity with Sable physics.")
                .defineInRange("creativeVectorThrusterMaxThrust", 6666.666666667d, 10.0d, 1000000.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("physics");
        NOZZLE_OFFSET_FROM_CENTER = COMMON_BUILDER.comment("Offset from the block center where force is applied.")
                .defineInRange("nozzleOffsetFromCenter", 0.45d, 0.0d, 1.5d);
        USE_ATMOSPHERIC_PRESSURE = COMMON_BUILDER.comment("If true, atmospheric pressure affects thruster output at altitude.")
                .define("useAtmosphericPressure", false);
        ATMOSPHERIC_PRESSURE_AMOUNT = COMMON_BUILDER.comment("Strength of atmospheric pressure influence. 1.0 = full effect, 0.0 = no effect.")
                .defineInRange("atmosphericPressureAmount", 1.0d, 0.0d, 2.0d);
        THRUST_UNITS_PER_KN = COMMON_BUILDER.comment(
                        "Shared thrust unit scale: how many internal thrust units correspond to 1 kN.",
                        "Used by physics conversion, tooltip display, and ComputerCraft kN helpers.")
                .defineInRange("thrustUnitsPerKn", 1000.0d, 1.0d, 1000000.0d);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("Stirling Engine");
        STIRLING_GENERATED_SU = COMMON_BUILDER.comment("Change this value to modify the amount of stress units produced by stirling engine. Value of 16 corresponds to 4096 SU.")
                .defineInRange("Generated stress units", 16.0, 1.0, 64.0);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("Tilt Adapter");
        TILT_ADAPTER_ANGLE_RANGE = COMMON_BUILDER.comment("Maximum absolute output angle in degrees, reached at full redstone differential.")
                .defineInRange("Maximum angle range", 45.0, 0.0, 180.0);
        ADVANCED_TILT_ADAPTER_MAX_ANGLE = COMMON_BUILDER.comment(
                        "Maximum configurable angle per side for the Advanced Tilt Adapter.")
                .defineInRange("Advanced maximum angle", 90, 1, 180);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("Burners");
        BURNERS_POWER_HEATED_MIXERS = COMMON_BUILDER.comment("If true - both solid and liquid burners can provide heat to heated mixers allowing for pre-nether brass.")
                .define("Burners power heated mixers", true);
        BURNERS_HEAT_STEAM_ENGINES = COMMON_BUILDER.comment("Allow propulsion burners to heat Create steam engines.")
                .define("Burners heat steam engines", true);
        BURNERS_SUPERHEAT_STEAM_ENGINES = COMMON_BUILDER.comment("Allow seething burners to count as superheated for steam engines.")
                .define("Burners superheat steam engines", true);
        BLAZE_BURNERS_HEAT_STIRLING_ENGINES = COMMON_BUILDER.comment("Allow vanilla blaze burners under stirling engines to provide heat.")
                .define("Blaze burners heat stirling engines", true);
        SOLID_BURNER_FUEL_CONSUMPTION_MULTIPLIER = COMMON_BUILDER.comment("Fuel consumption multiplier for solid burners. Higher values make inserted items burn faster.")
                .defineInRange("Solid burner fuel consumption multiplier", 1.0, 0.01, 100.0);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("Cable");
        CABLE_ENERGY_TRANSFER = COMMON_BUILDER.comment("Maximum FE moved per tick by a single cable block.")
                .defineInRange("Energy transfer", 1_000, 1, 100000000);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("Fuel Configuration");
        COMMON_BUILDER.comment(
                "Fuel properties by fluid id. Configure efficiency and burn rate separately as percentages.");
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

        COMMON_BUILDER.comment(
                "Coral conversion entries. Each key is a fluid id and each value is '<fe_per_mb>'.",
                "Example value: 16");
        COMMON_BUILDER.push("coralFuelConversionRates");
        for (String entry : defaultCoralFuelConversionRates()) {
            String[] split = entry.split("=", 2);
            if (split.length != 2) continue;
            CORAL_FUEL_CONVERSION_RATE_ENTRIES.put(split[0], COMMON_BUILDER.define(configKeyForFluidId(split[0]), split[1]));
        }
        COMMON_BUILDER.pop();

        ADDITIONAL_THRUSTER_FUEL_PROPERTY_LINES = COMMON_BUILDER.comment(
                        "Additional thruster fuel lines (same format as defaults: fluid_id=efficiencyPercent,burnRatePercent).",
                        "Use for fluids that do not have a fuelProperties subsection. Entries here override matching fluids from the table above.")
                .defineListAllowEmpty("additionalThrusterFuelLines", ArrayList::new, obj -> obj instanceof String);

        COMMON_BUILDER.pop();

        COMMON_BUILDER.push("thrusterDyeColors");
        COMMON_BUILDER.comment("Particle color overrides when a dye is applied to a thruster. Values are RRGGBB hex strings.");
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
                    COMMON_BUILDER.define(e[0], e[1]));
        }
        COMMON_BUILDER.pop();

        PropulsionDefaultStress.INSTANCE.registerAll(COMMON_BUILDER);

        COMMON_SPEC = COMMON_BUILDER.build();
        //#endregion

        //#region Client
        CLIENT_BUILDER.push("Stirling Engine");
        STIRLING_REVOLUTION_PERIOD = CLIENT_BUILDER.comment("Revolution period of the simulated shaft (affects only piston movement).")
                .define("Revolution period", 0.2);
        STIRLING_CRANK_RADIUS = CLIENT_BUILDER.comment("Radius of the simulated crank.")
                .define("Crank radius", 0.125);
        STIRLING_CONROD_LENGTH = CLIENT_BUILDER.comment("Length of the simulated conrod.")
                .define("Conrod length", 0.5);
        CLIENT_BUILDER.pop();


        CLIENT_BUILDER.push("Debug");
        DEBUG_THRUSTER = CLIENT_BUILDER.comment("Render thruster debug overlays (plume ray, obstruction hits, damage zones).")
                .define("Thruster", false);
        CLIENT_BUILDER.pop();

        CLIENT_BUILDER.push("Thruster Plumes");
        PLUME_RENDER_MODE = CLIENT_BUILDER.comment("How every thruster plume is rendered. Particle uses normal particle effects; Shader uses the procedural plume shader.")
                .defineEnum("renderMode", PlumeRenderMode.PARTICLE);
        CLIENT_BUILDER.pop();


        CLIENT_SPEC = CLIENT_BUILDER.build();
        //#endregion
    }

    private static List<String> defaultFuelProperties() {
        return new ArrayList<>(List.of(
                "createpropulsion:turpentine=100,150",
                "minecraft:lava=75,100",
                "createdieselgenerators:plant_oil=55,170",
                "immersiveengineering:plantoil=55,170",
                "createdieselgenerators:ethanol=70,140",
                "immersiveengineering:ethanol=70,140",
                "mekanismgenerators:bioethanol=75,135",
                "northstar:biofuel=80,125",
                "createdieselgenerators:biodiesel=90,110",
                "immersiveengineering:biodiesel=90,110",
                "immersiveengineering:high_power_biodiesel=105,95",
                "createdieselgenerators:diesel=100,100",
                "tfmg:diesel=100,100",
                "stellaris:diesel=100,100",
                "tfmg:naphtha=95,105",
                "tfmg:kerosene=230,90",
                "createdieselgenerators:gasoline=125,80",
                "tfmg:gasoline=125,80",
                "tfmg:lpg=120,85",
                "northstar:hydrocarbon=130,75",
                "stellaris:fuel=115,100",
                "mekanism:hydrogen=230,90",
                "createaddition:bioethanol=75,135",
                "createaddition:seed_oil=55,170",
                "northstar:methane=105,95",
                "northstar:liquid_hydrogen=230,90",
                "immersivepetroleum:diesel_sulfur=100,100"
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
        try {
            return defaultCoralFuelConversionRates();
        } catch (IllegalStateException ignored) {
            return defaultCoralFuelConversionRates();
        }
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
            List<? extends String> extra = ADDITIONAL_THRUSTER_FUEL_PROPERTY_LINES.get();
            if (extra != null) {
                for (Object o : extra) {
                    if (!(o instanceof String raw)) {
                        continue;
                    }
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
        try {
            return LIQUID_VECTOR_THRUSTER_BASE_THRUST.get();
        } catch (IllegalStateException ignored) {
            return VECTOR_THRUSTER_BASE_THRUST.get();
        }
    }

    public static int getLiquidVectorThrusterFuelTankCapacityMbOrDefault() {
        try {
            return LIQUID_VECTOR_THRUSTER_FUEL_TANK_CAPACITY_MB.get();
        } catch (IllegalStateException ignored) {
            return FUEL_TANK_CAPACITY_MB.get();
        }
    }

    public static double getLiquidVectorThrusterFuelMbPerTickAtFullThrottleOrDefault() {
        try {
            return LIQUID_VECTOR_THRUSTER_FUEL_MB_PER_TICK_AT_FULL_THROTTLE.get();
        } catch (IllegalStateException ignored) {
            return FUEL_MB_PER_TICK_AT_FULL_THROTTLE.get();
        }
    }

    public static double getThrustUnitsPerKnOrDefault() {
        try {
            return THRUST_UNITS_PER_KN.get();
        } catch (IllegalStateException ignored) {
            return 1000.0d;
        }
    }


    public static boolean useShaderPlumes() {
        try {
            return PLUME_RENDER_MODE.get() == PlumeRenderMode.SHADER;
        } catch (IllegalStateException ignored) {
            // Client configs are not loaded on dedicated servers. Particle plumes are the
            // safe fallback because server-side thruster ticking uses this value too.
            return false;
        }
    }

}
