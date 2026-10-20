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
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockLootResourcesTest {
    @Test
    void cableBlocksAndCasingDropThemselves() throws IOException {
        for (String block : List.of("cable", "cable_hub", "cable_relay", "platinum_casing")) {
            JsonObject loot = load("createpropulsion/loot_table/blocks/" + block + ".json");
            assertEquals("minecraft:block", loot.get("type").getAsString());
            JsonObject pool = loot.getAsJsonArray("pools").get(0).getAsJsonObject();
            assertEquals(1, pool.get("rolls").getAsInt());
            JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("minecraft:item", entry.get("type").getAsString());
            assertEquals("createpropulsion:" + block, entry.get("name").getAsString());
        }
    }

    @Test
    void cableBlocksAndCasingSupportPickaxesAndWrenchPickup() throws IOException {
        for (String tag : List.of("minecraft/tags/block/mineable/pickaxe.json", "create/tags/block/wrench_pickup.json")) {
            var values = load(tag).getAsJsonArray("values");
            for (String block : List.of("cable", "cable_hub", "cable_relay", "platinum_casing")) {
                assertTrue(values.asList().stream().anyMatch(value -> value.getAsString().equals("createpropulsion:" + block)), block);
            }
        }
    }

    private static JsonObject load(String path) throws IOException {
        var stream = BlockLootResourcesTest.class.getClassLoader().getResourceAsStream("data/" + path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
