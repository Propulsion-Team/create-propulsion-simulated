package dev.propulsionteam.propulsionsimulated.content.heat;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ThermostatTargetsTest {
    @Test
    void compatibilityTargetsAreOptionalAndAllowDatapackExtensions() throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(
            "data/createpropulsion/tags/block/thermostat_targets.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var tag = JsonParser.parseReader(reader).getAsJsonObject();
            assertFalse(tag.get("replace").getAsBoolean());
            Set<String> targets = new HashSet<>();
            for (var value : tag.getAsJsonArray("values")) {
                var entry = value.getAsJsonObject();
                assertFalse(entry.get("required").getAsBoolean());
                targets.add(entry.get("id").getAsString());
            }
            assertEquals(Set.of("create_connected:fluid_vessel", "createmetallurgy:industrial_crucible"), targets);
        }
    }
}
