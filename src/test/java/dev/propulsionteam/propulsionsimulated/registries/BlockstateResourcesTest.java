package dev.propulsionteam.propulsionsimulated.registries;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BlockstateResourcesTest {
    @Test
    void fluidModelsCoverEveryLevelWithoutRenderingSolidGeometry() throws IOException {
        for (String fluid : List.of("turpentine", "coral", "oxidizer")) {
            JsonObject variants = load("blockstates/" + fluid + ".json").getAsJsonObject("variants");
            assertEquals(1, variants.size());
            String model = variants.getAsJsonObject("").get("model").getAsString();
            if (fluid.equals("coral")) {
                assertEquals("createpropulsion:block/coral", model);
                JsonObject coralModel = load("models/block/coral.json");
                assertEquals(1, coralModel.size());
                assertEquals("createpropulsion:block/coral_still",
                    coralModel.getAsJsonObject("textures").get("particle").getAsString());
            } else if (fluid.equals("turpentine")) {
                assertEquals("createpropulsion:block/turpentine", model);
                assertEquals(1, load("models/block/turpentine.json").size());
            } else {
                assertEquals("minecraft:block/water", model);
            }
        }
    }

    @Test
    void tiltAdaptersCoverAllAxisAndFacingCombinations() throws IOException {
        for (String adapter : List.of("tilt_adapter", "advanced_tilt_adapter")) {
            JsonObject variants = load("blockstates/" + adapter + ".json").getAsJsonObject("variants");
            assertEquals(36, variants.size());
            for (String axis : List.of("x", "y", "z")) {
                for (boolean alongFirst : List.of(false, true)) {
                    for (String facing : List.of("north", "south", "east", "west", "up", "down")) {
                        String key = "axis=" + axis + ",axis_along_first=" + alongFirst + ",facing=" + facing;
                        JsonObject variant = variants.getAsJsonObject(key);
                        assertNotNull(variant, adapter + ": " + key);
                        String model = variant.get("model").getAsString();
                        load("models/" + model.substring("createpropulsion:".length()) + ".json");
                    }
                }
            }
        }
    }

    private static JsonObject load(String path) throws IOException {
        String resource = "assets/createpropulsion/" + path;
        var stream = BlockstateResourcesTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(stream, resource);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
