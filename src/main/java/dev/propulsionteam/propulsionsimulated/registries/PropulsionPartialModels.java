package dev.propulsionteam.propulsionsimulated.registries;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class PropulsionPartialModels {
    //Lodestone
    public static final PartialModel LODESTONE_TRACKER_INDICATOR = partial("lodestone_tracker_overlay");
    //Reaction wheel
    public static final PartialModel REACTION_WHEEL_CORE = partial("reaction_wheel_core");
    //Stirling engine
    public static final PartialModel STIRLING_ENGINE_PISTON = partial("stirling_piston");
    //Liquid burner
    public static final PartialModel LIQUID_BURNER_FAN = partial("liquid_burner_fan");
    //Tilt adapter
    public static final PartialModel TILT_ADAPTER_INPUT_SHAFT = partial("tilt_adapter_input_shaft");
    public static final PartialModel TILT_ADAPTER_OUTPUT_SHAFT = partial("tilt_adapter_output_shaft");
    public static final PartialModel TILT_ADAPTER_GANTRY = partial("tilt_adapter_screw_overlay");
    public static final PartialModel TILT_ADAPTER_SIDE_INDICATOR = partial("tilt_adapter_side_overlay");
    //Creative thruster
    public static final PartialModel CREATIVE_THRUSTER_BRACKET = partial("creative_thruster_bracket");
    //Vector thruster
    public static final PartialModel VECTOR_THRUSTER_MOVING_ASSEMBLY = partial("vector_thruster_moving_assembly");
    public static final PartialModel VECTOR_THRUSTER_BODY = partial("vector_thruster_body");
    public static final PartialModel VECTOR_THRUSTER_FLAP_TOP = partial("vector_thruster_flap_top");
    public static final PartialModel VECTOR_THRUSTER_FLAP_BOTTOM = partial("vector_thruster_flap_bottom");
    public static final PartialModel VECTOR_THRUSTER_FLAP_LEFT = partial("vector_thruster_flap_left");
    public static final PartialModel VECTOR_THRUSTER_FLAP_RIGHT = partial("vector_thruster_flap_right");
    public static final PartialModel LIQUID_VECTOR_THRUSTER_MOVING_ASSEMBLY = partial("liquid_vector_thruster_moving_assembly");
    public static final PartialModel LIQUID_VECTOR_THRUSTER_BODY = partial("liquid_vector_thruster_body");
    public static final PartialModel LIQUID_VECTOR_THRUSTER_FLAP_TOP = partial("liquid_vector_thruster_flap_top");
    public static final PartialModel LIQUID_VECTOR_THRUSTER_FLAP_BOTTOM = partial("liquid_vector_thruster_flap_bottom");
    public static final PartialModel LIQUID_VECTOR_THRUSTER_FLAP_LEFT = partial("liquid_vector_thruster_flap_left");
    public static final PartialModel LIQUID_VECTOR_THRUSTER_FLAP_RIGHT = partial("liquid_vector_thruster_flap_right");
    public static final PartialModel CREATIVE_VECTOR_THRUSTER_MOVING_ASSEMBLY = partial("creative_vector_thruster_moving_assembly");
    public static final PartialModel CREATIVE_VECTOR_THRUSTER_BODY = partial("creative_vector_thruster_body");
    public static final PartialModel CREATIVE_VECTOR_THRUSTER_FLAP_TOP = partial("creative_vector_thruster_flap_top");
    public static final PartialModel CREATIVE_VECTOR_THRUSTER_FLAP_BOTTOM = partial("creative_vector_thruster_flap_bottom");
    public static final PartialModel CREATIVE_VECTOR_THRUSTER_FLAP_LEFT = partial("creative_vector_thruster_flap_left");
    public static final PartialModel CREATIVE_VECTOR_THRUSTER_FLAP_RIGHT = partial("creative_vector_thruster_flap_right");
    //Multiblock thruster
    public static final PartialModel THRUSTER_MULTIBLOCK_2X2X2 = partial("thruster_multiblock_2x2x2");
    public static final PartialModel THRUSTER_MULTIBLOCK_3X3X3 = partial("thruster_multiblock_3x3x3");
    public static final PartialModel CREATIVE_THRUSTER_MULTIBLOCK_2X2X2 = partial("creative_thruster_multiblock_2x2x2");
    public static final PartialModel CREATIVE_THRUSTER_MULTIBLOCK_3X3X3 = partial("creative_thruster_multiblock_3x3x3");
    public static final PartialModel ION_THRUSTER_MULTIBLOCK_2X2X2 = partial("2x2x2_ion_thruster");
    public static final PartialModel ION_THRUSTER_MULTIBLOCK_3X3X3 = partial("3x3x3_ion_thruster");
    //Transmission
    public static final PartialModel TRANSMISSION_PLUS = partial("transmission_plus");
    public static final PartialModel TRANSMISSION_MINUS = partial("transmission_minus");
    public static final PartialModel THRUSTER_PLUME_BASE = partial("thruster_plume_base");
    public static final PartialModel THRUSTER_PLUME_JET = partial("thruster_plume_jet");

    private static PartialModel partial(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "partial/" + path));
    }

    public static void register() {}
}
