package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ComputerCraftConfig {
    private static final String PACKAGE_PREFIX = "dev.propulsionteam.propulsionsimulated.compat.computercraft.";
    private static final List<String> THRUSTER_BASE = List.of(
            "setThrustPN", "toggleVectorCore", "setVectorCoreEnabled", "isVectorCoreEnabled",
            "getMaxThrustPN", "getObstructionEfficiency");
    private static final List<String> VECTOR_BASE = join(THRUSTER_BASE, List.of(
            "getVectorX", "getVectorY", "getTargetVectorX", "getTargetVectorY",
            "setVectorX", "setVectorY", "setVector", "setThrust", "setThrustNormalized",
            "setPower", "setPowerNormalized", "getThrust", "getPower"));
    private static final List<String> FLUID = List.of("tanks", "pushFluid", "pullFluid");

    public record Group(boolean api, String name, String className, List<String> methods) {}

    public static final List<Group> GROUPS = List.of(
            api("ship", "ShipAPI",
                    "getComputerAxes", "isOnShip", "getId", "getName", "getPosition", "getComputerPosition",
                    "getMass", "getInertiaTensor", "getVelocity", "getAirVelocity", "getAngularVelocity",
                    "getGravity", "getEuler", "getOrientation", "getState"),
            api("thrusters", "ThrustersAPI",
                    "list", "get", "getNetForce", "getNetTorque", "setThrust", "setThrottle", "setAll",
                    "setVector", "setVectorCores", "release", "allocate"),
            api("links", "LinksAPI",
                    "sendLinkSignal", "sendLinkSignals", "getActiveSignals", "getLinkSignal", "listen",
                    "unlisten", "scan", "getRange", "clearAll"),
            peripheral("thruster", "ThrusterPeripheral", join(THRUSTER_BASE, List.of(
                    "getObstruction", "setPower", "setPowerNormalized", "getPower", "getCurrentThrustPN",
                    "getAirflowMs", "getFuelAmountMb", "getFuelCapacityMb"), FLUID)),
            peripheral("solid_fuel_thruster", "SolidFuelThrusterPeripheral", join(THRUSTER_BASE, List.of(
                    "getObstruction", "setPower", "setPowerNormalized", "getPower", "getCurrentThrustPN",
                    "getAirflowMs", "getFuelItem", "getBurnTimeRemaining", "isBurning", "list", "pushItems",
                    "pullItems"))),
            peripheral("ion_thruster", "IonThrusterPeripheral", join(THRUSTER_BASE, List.of(
                    "getObstruction", "setPower", "setPowerNormalized", "getPower", "getCurrentThrustPN",
                    "getAirflowMs", "getEnergyAmountFe", "getEnergyCapacityFe"))),
            peripheral("creative_thruster", "CreativeThrusterPeripheral", join(THRUSTER_BASE, List.of(
                    "getObstruction", "setPower", "setPowerNormalized", "getPower", "setThrustConfig",
                    "getThrustConfig", "getTargetThrustPN", "getCurrentThrustPN", "getAirflowMs"))),
            peripheral("vector_thruster", "VectorThrusterPeripheral", VECTOR_BASE),
            peripheral("liquid_vector_thruster", "LiquidVectorThrusterPeripheral", join(VECTOR_BASE, FLUID)),
            peripheral("creative_vector_thruster", "CreativeVectorThrusterPeripheral", join(VECTOR_BASE, List.of(
                    "setThrustOutput", "clearThrustOutput", "getMaxThrustOutputPn",
                    "isCustomThrustOutputActive"))),
            peripheral("rcs_thruster", "RcsThrusterPeripheral", join(List.of(
                    "getNozzleCount", "setNozzlePower", "setNozzlePowerNormalized", "setNozzleThrustPN",
                    "getNozzlePower", "getNozzleThrustPN", "getCurrentThrustPN", "getMaxThrustPN",
                    "toggleVectorCore", "setVectorCoreEnabled", "isVectorCoreEnabled", "isBoosted",
                    "getFuelAmountMb", "getFuelCapacityMb"), FLUID)),
            peripheral("lidar", "LidarPeripheral", List.of(
                    "getMaxRange", "getMaxAngle", "getDirection", "getMaxScanRays", "raycast", "scan")),
            peripheral("hydropump", "HydropumpPeripheral", List.of(
                    "getPumpingSpeed", "setPumpingSpeed", "getMaxPumpingSpeed")),
            peripheral("stirling_engine", "StirlingEnginePeripheral", List.of("getRpm", "setSpeed", "setActive")),
            peripheral("coral_generator", "CoralGeneratorPeripheral", List.of(
                    "getCoralAmountMb", "getCoralCapacityMb", "getEnergyAmountFe", "getEnergyCapacityFe")),
            peripheral("redstone_transmission", "RedstoneTransmissionPeripheral", List.of(
                    "getTransmissionMode", "setTransmissionMode", "getShiftLevel", "setShiftLevel")));

    private record Toggles(ModConfigSpec.BooleanValue enabled, Map<String, ModConfigSpec.BooleanValue> methods) {}

    private static final Map<String, Toggles> TOGGLES = new LinkedHashMap<>();

    private ComputerCraftConfig() {}

    public static void define(ModConfigSpec.Builder builder) {
        builder.comment(
                        "CC:Tweaked integration. Disabled APIs and methods are hidden from Lua.",
                        "Reboot computers (and reload the world for disabled peripherals) after changing these.")
                .push("computercraft");
        defineSection(builder, true, "apis", "Global Lua APIs available on computers.");
        defineSection(builder, false, "peripherals", "Block peripherals. A disabled peripheral is treated by "
                + "ComputerCraft like a block without one, so its generic inventory, fluid and energy methods may "
                + "still appear.");
        builder.pop();
    }

    private static void defineSection(ModConfigSpec.Builder builder, boolean api, String section, String comment) {
        builder.comment(comment).push(section);
        for (Group group : GROUPS) {
            if (group.api() != api) continue;
            builder.push(group.name());
            ModConfigSpec.BooleanValue enabled = builder
                    .comment("Enable the " + group.name() + (api ? " API." : " peripheral."))
                    .define("enabled", true);
            Map<String, ModConfigSpec.BooleanValue> methods = new LinkedHashMap<>();
            builder.push("methods");
            for (String method : group.methods()) {
                methods.put(method, builder.define(method, true));
            }
            builder.pop();
            builder.pop();
            TOGGLES.put(PACKAGE_PREFIX + group.className(), new Toggles(enabled, methods));
        }
        builder.pop();
    }

    public static boolean isEnabled(Class<?> type) {
        Toggles toggles = TOGGLES.get(type.getName());
        return toggles == null || get(toggles.enabled());
    }

    public static boolean isMethodEnabled(Class<?> type, String method) {
        Toggles toggles = TOGGLES.get(type.getName());
        if (toggles == null) return true;
        ModConfigSpec.BooleanValue value = toggles.methods().get(method);
        return value == null || get(value);
    }

    private static boolean get(ModConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return value.getDefault();
        }
    }

    private static Group api(String name, String className, String... methods) {
        return new Group(true, name, className, List.of(methods));
    }

    private static Group peripheral(String name, String className, List<String> methods) {
        return new Group(false, name, className, methods);
    }

    @SafeVarargs
    private static List<String> join(List<String>... lists) {
        List<String> out = new ArrayList<>();
        for (List<String> list : lists) {
            out.addAll(list);
        }
        return List.copyOf(out);
    }
}
