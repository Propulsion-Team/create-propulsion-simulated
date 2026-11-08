package dev.propulsionteam.propulsionsimulated.registries;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecipeResourcesTest {
    @Test
    void soundMufflerUsesIronSheetsAndAnyWool() throws IOException {
        JsonObject recipe = load("crafting/sound_muffler");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        var pattern = recipe.getAsJsonArray("pattern");
        assertEquals(List.of(" I ", "IWI", " I "),
                pattern.asList().stream().map(element -> element.getAsString()).toList());
        assertEquals("create:iron_sheet", recipe.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
        assertEquals("minecraft:wool", recipe.getAsJsonObject("key").getAsJsonObject("W").get("tag").getAsString());
        assertEquals("createpropulsion:sound_muffler", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void thrusterRecipesMatchTheirCraftingGrids() throws IOException {
        assertShaped("rcs_thruster",
                "create:iron_sheet,create:iron_sheet,create:iron_sheet",
                "create:chute,minecraft:redstone_block,create:chute",
                "create:redstone_link,create:copper_sheet,create:redstone_link");
        assertShaped("single_rcs_thruster",
                ",create:chute,",
                "create:iron_sheet,minecraft:redstone_block,create:iron_sheet",
                "create:redstone_link,create:copper_sheet,create:iron_sheet");
        assertShaped("thruster",
                "create:copper_sheet,create:portable_fluid_interface,create:copper_sheet",
                "create:iron_sheet,create:fluid_tank,create:iron_sheet",
                "create:iron_sheet,create:chute,create:iron_sheet");
        assertShaped("ion_thruster",
                "create:brass_sheet,create:crafter_slot_cover,create:brass_sheet",
                "create:sturdy_sheet,simulated:redstone_magnet,create:sturdy_sheet",
                "create:sturdy_sheet,create:chute,create:sturdy_sheet");
        assertShaped("vector_thruster",
                "createpropulsion:platinum_sheet,,createpropulsion:platinum_sheet",
                "create:sturdy_sheet,createpropulsion:ion_thruster,create:sturdy_sheet",
                "create:redstone_link,simulated:gyroscopic_mechanism,create:redstone_link");
        assertShaped("liquid_vector_thruster",
                "create:copper_sheet,,create:copper_sheet",
                "create:sturdy_sheet,createpropulsion:thruster,create:sturdy_sheet",
                "create:redstone_link,simulated:gyroscopic_mechanism,create:redstone_link");
    }

    @Test
    void engineAndBurnerRecipesMatchTheirCraftingGrids() throws IOException {
        assertShaped("stirling_engine",
                "create:iron_sheet,create:shaft,create:iron_sheet",
                "create:andesite_alloy,create:precision_mechanism,create:andesite_alloy",
                "create:copper_sheet,create:copper_sheet,create:copper_sheet");
        assertShaped("solid_burner",
                "create:copper_sheet,create:copper_sheet,create:copper_sheet",
                "create:andesite_alloy,minecraft:blast_furnace,create:andesite_alloy",
                "create:iron_sheet,create:iron_sheet,create:iron_sheet");
        assertShaped("liquid_burner",
                "create:copper_sheet,create:copper_sheet,create:copper_sheet",
                "create:chute,create:blaze_burner,create:propeller",
                "create:iron_sheet,create:fluid_pipe,create:iron_sheet");
    }

    @Test
    void copycatWingsAreStonecutFromZinc() throws IOException {
        JsonObject recipe = load("stonecutting/copycat_wing_from_zinc");
        assertEquals("minecraft:stonecutting", recipe.get("type").getAsString());
        assertEquals("create:zinc_ingot", recipe.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals("createpropulsion:copycat_wing", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(4, recipe.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void copycatWingsCannotBeSawedFromWings() {
        assertNull(RecipeResourcesTest.class.getClassLoader()
                .getResource("data/createpropulsion/recipe/cutting/copycat_wing.json"));
    }

    @Test
    void regularAndTemperedWingRecipesAreRemoved() {
        for (String path : List.of("crafting/wing", "crafting/wing_from_symmetric_wing",
                "crafting/symmetric_wing_from_wing", "smelting/tempered_wing")) {
            assertNull(RecipeResourcesTest.class.getClassLoader()
                    .getResource("data/createpropulsion/recipe/" + path + ".json"), path);
        }
    }

    @Test
    void coralUsesTurpentineInsteadOfHealingPotion() throws IOException {
        JsonObject recipe = load("mixing/coral");
        assertEquals("create:mixing", recipe.get("type").getAsString());
        assertEquals("superheated", recipe.get("heat_requirement").getAsString());
        var ingredients = recipe.getAsJsonArray("ingredients");
        assertEquals(3, ingredients.size());
        JsonObject fluid = ingredients.get(0).getAsJsonObject();
        assertEquals("neoforge:single", fluid.get("type").getAsString());
        assertEquals("createpropulsion:turpentine", fluid.get("fluid").getAsString());
        assertEquals(500, fluid.get("amount").getAsInt());
        assertFalse(fluid.has("components"));
        assertEquals("minecraft:redstone", ingredients.get(1).getAsJsonObject().get("item").getAsString());
        assertEquals("minecraft:blaze_powder", ingredients.get(2).getAsJsonObject().get("item").getAsString());
        assertFluidResult(recipe, "coral", 500);
    }

    @Test
    void oxidizerUsesFourIronNuggetsWithoutHeating() throws IOException {
        JsonObject recipe = load("mixing/oxidizer");
        assertEquals("create:mixing", recipe.get("type").getAsString());
        assertFalse(recipe.has("heat_requirement"));
        var ingredients = recipe.getAsJsonArray("ingredients");
        assertEquals(6, ingredients.size());
        assertEquals("minecraft:nether_wart", ingredients.get(0).getAsJsonObject().get("item").getAsString());
        for (int i = 1; i <= 4; i++) {
            assertEquals("minecraft:iron_nugget", ingredients.get(i).getAsJsonObject().get("item").getAsString());
        }
        JsonObject water = ingredients.get(5).getAsJsonObject();
        assertEquals("neoforge:single", water.get("type").getAsString());
        assertEquals("minecraft:water", water.get("fluid").getAsString());
        assertEquals(1000, water.get("amount").getAsInt());
        assertFluidResult(recipe, "oxidizer", 1000);
    }

    private static void assertShaped(String name, String... rows) throws IOException {
        JsonObject recipe = load("crafting/" + name);
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString(), name);
        var pattern = recipe.getAsJsonArray("pattern");
        var key = recipe.getAsJsonObject("key");
        var usedKeys = new HashSet<String>();
        assertEquals(rows.length, pattern.size(), name);
        for (int row = 0; row < rows.length; row++) {
            String[] expected = rows[row].split(",", -1);
            String actual = pattern.get(row).getAsString();
            assertEquals(expected.length, actual.length(), name);
            for (int column = 0; column < expected.length; column++) {
                String symbol = String.valueOf(actual.charAt(column));
                if (expected[column].isEmpty()) {
                    assertEquals(" ", symbol, name);
                } else {
                    usedKeys.add(symbol);
                    assertNotNull(key.get(symbol), name + ": " + symbol);
                    assertEquals(expected[column], key.getAsJsonObject(symbol).get("item").getAsString(), name);
                }
            }
        }
        assertEquals(usedKeys, key.keySet(), name);
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("createpropulsion:" + name, result.get("id").getAsString());
        assertEquals(1, result.has("count") ? result.get("count").getAsInt() : 1, name);
    }

    private static void assertFluidResult(JsonObject recipe, String fluid, int amount) {
        var results = recipe.getAsJsonArray("results");
        assertEquals(1, results.size());
        JsonObject result = results.get(0).getAsJsonObject();
        assertEquals("createpropulsion:" + fluid, result.get("id").getAsString());
        assertEquals(amount, result.get("amount").getAsInt());
    }

    private static JsonObject load(String path) throws IOException {
        var stream = RecipeResourcesTest.class.getClassLoader()
                .getResourceAsStream("data/createpropulsion/recipe/" + path + ".json");
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
