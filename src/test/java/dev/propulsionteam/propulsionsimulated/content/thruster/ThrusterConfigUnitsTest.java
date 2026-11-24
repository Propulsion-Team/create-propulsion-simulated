package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThrusterConfigUnitsTest {
    private static final Map<String, ModConfigSpec.DoubleValue> BASE_THRUSTS = Map.of(
        "baseThrust", PropulsionConfig.BASE_THRUST,
        "ionThrusterBaseThrust", PropulsionConfig.ION_THRUSTER_BASE_THRUST,
        "creativeThrusterBaseThrust", PropulsionConfig.CREATIVE_THRUSTER_BASE_THRUST,
        "vectorThrusterBaseThrust", PropulsionConfig.VECTOR_THRUSTER_BASE_THRUST,
        "liquidVectorThrusterBaseThrust", PropulsionConfig.LIQUID_VECTOR_THRUSTER_BASE_THRUST,
        "solidFuelThrusterBaseThrust", PropulsionConfig.SOLID_FUEL_THRUSTER_BASE_THRUST,
        "creativeVectorThrusterBaseThrust", PropulsionConfig.CREATIVE_VECTOR_THRUSTER_BASE_THRUST
    );

    @Test
    void defaultBaseThrustShowsExactlyTheSameNumberInTheTooltip() {
        double units = PropulsionConfig.THRUST_UNITS_PER_PN.getDefault();
        BASE_THRUSTS.forEach((name, value) -> {
            double config = value.getDefault();
            float internal = (float) (config * units);
            double tooltip = internal / units;
            assertEquals(config, tooltip, 0.0, name);
            assertEquals(String.format("%.2f", config), String.format("%.2f", tooltip), name);
        });
    }

    @Test
    void defaultBaseThrustHasNoFractionalDigits() {
        BASE_THRUSTS.forEach((name, value) -> {
            double config = value.getDefault();
            assertEquals(Math.rint(config), config, 0.0, name);
        });
    }
}
