package dev.propulsionteam.propulsionsimulated.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlatinumWorldgenTest {
    // Minecraft 1.21.1's four Overworld diamond feature distributions.
    private static final List<FeatureExpectation> FEATURES = List.of(
            new FeatureExpectation("ore_platinum", 4, 0.5, "count", 7,
                    "trapezoid", "above_bottom", -80, 80),
            new FeatureExpectation("ore_platinum_medium", 8, 0.5, "count", 2,
                    "uniform", "absolute", -64, -4),
            new FeatureExpectation("ore_platinum_large", 12, 0.7, "rarity_filter", 9,
                    "trapezoid", "above_bottom", -80, 80),
            new FeatureExpectation("ore_platinum_buried", 8, 1.0, "count", 4,
                    "trapezoid", "above_bottom", -80, 80)
    );

    @Test
    void configuredFeaturesMatchVanillaDiamondGeneration() throws IOException {
        for (FeatureExpectation expectation : FEATURES) {
            JsonObject root = load("data/createpropulsion/worldgen/configured_feature/"
                    + expectation.name() + ".json").getAsJsonObject();
            JsonObject config = root.getAsJsonObject("config");

            assertEquals("minecraft:ore", root.get("type").getAsString());
            assertEquals(expectation.veinSize(), config.get("size").getAsInt());
            assertEquals(expectation.airDiscardChance(),
                    config.get("discard_chance_on_air_exposure").getAsDouble());

            JsonArray targets = config.getAsJsonArray("targets");
            assertEquals(2, targets.size());
            assertTarget(targets.get(0).getAsJsonObject(), "createpropulsion:platinum_ore",
                    "minecraft:stone_ore_replaceables");
            assertTarget(targets.get(1).getAsJsonObject(), "createpropulsion:deepslate_platinum_ore",
                    "minecraft:deepslate_ore_replaceables");
        }
    }

    @Test
    void placedFeaturesMatchVanillaDiamondGeneration() throws IOException {
        for (FeatureExpectation expectation : FEATURES) {
            JsonObject root = load("data/createpropulsion/worldgen/placed_feature/"
                    + expectation.name() + ".json").getAsJsonObject();
            JsonArray placement = root.getAsJsonArray("placement");

            assertEquals("createpropulsion:" + expectation.name(), root.get("feature").getAsString());
            assertEquals(4, placement.size());
            assertEquals("minecraft:" + expectation.frequencyType(),
                    placement.get(0).getAsJsonObject().get("type").getAsString());
            assertEquals(expectation.frequency(), placement.get(0).getAsJsonObject()
                    .get(expectation.frequencyType().equals("count") ? "count" : "chance").getAsInt());
            assertEquals("minecraft:in_square", placement.get(1).getAsJsonObject().get("type").getAsString());
            assertHeightRange(placement.get(2).getAsJsonObject(), expectation);
            assertEquals("minecraft:biome", placement.get(3).getAsJsonObject().get("type").getAsString());
        }
    }

    @Test
    void biomeModifiersIncludeConventionOverworldBiomes() throws IOException {
        for (FeatureExpectation expectation : FEATURES) {
            JsonObject modifier = load("data/createpropulsion/neoforge/biome_modifier/add_"
                    + expectation.name() + ".json").getAsJsonObject();

            assertEquals("neoforge:add_features", modifier.get("type").getAsString());
            assertEquals("#c:is_overworld", modifier.get("biomes").getAsString());
            assertEquals("underground_ores", modifier.get("step").getAsString());
            assertEquals("createpropulsion:" + expectation.name(), modifier.get("features").getAsString());
        }
    }

    private static void assertTarget(JsonObject target, String block, String replaceableTag) {
        assertEquals(block, target.getAsJsonObject("state").get("Name").getAsString());
        JsonObject rule = target.getAsJsonObject("target");
        assertEquals("minecraft:tag_match", rule.get("predicate_type").getAsString());
        assertEquals(replaceableTag, rule.get("tag").getAsString());
    }

    private static void assertHeightRange(JsonObject placement, FeatureExpectation expectation) {
        assertEquals("minecraft:height_range", placement.get("type").getAsString());
        JsonObject height = placement.getAsJsonObject("height");
        assertEquals("minecraft:" + expectation.heightType(), height.get("type").getAsString());
        assertEquals(expectation.minimum(), height.getAsJsonObject("min_inclusive")
                .get(expectation.anchorType()).getAsInt());
        assertEquals(expectation.maximum(), height.getAsJsonObject("max_inclusive")
                .get(expectation.anchorType()).getAsInt());
    }

    private static JsonElement load(String resourcePath) throws IOException {
        InputStream stream = PlatinumWorldgenTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assertNotNull(stream, "Missing worldgen resource " + resourcePath);
        try (stream; Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    private record FeatureExpectation(String name, int veinSize, double airDiscardChance,
                                      String frequencyType, int frequency, String heightType,
                                      String anchorType, int minimum, int maximum) {
    }
}
