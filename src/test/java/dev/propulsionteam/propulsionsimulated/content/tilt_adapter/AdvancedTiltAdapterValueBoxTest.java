package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdvancedTiltAdapterValueBoxTest {
    @Test
    void boxPositionsFollowTheRenderedModelInEveryOrientation() throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(
            "assets/createpropulsion/blockstates/advanced_tilt_adapter.json");
        assertNotNull(stream);
        JsonObject variants;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            variants = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("variants");
        }
        for (var entry : variants.entrySet()) {
            String key = entry.getKey();
            Direction facing = Direction.byName(key.substring(key.lastIndexOf('=') + 1));
            boolean alongFirst = key.contains("axis_along_first=true");
            JsonObject variant = entry.getValue().getAsJsonObject();
            int xRotation = variant.has("x") ? variant.get("x").getAsInt() : 0;
            int yRotation = variant.has("y") ? variant.get("y").getAsInt() : 0;
            var rotation = BlockModelRotation.by(xRotation, yRotation).getRotation().getLeftRotation();
            for (float x : new float[]{3f, 13f}) {
                var onModelTop = new Vec3(x / 16f, 1, 0.5);
                var expected = new Vector3f((float) onModelTop.x - 0.5f, 0.5f, 0);
                rotation.transform(expected);
                expected.add(0.5f, 0.5f, 0.5f);
                Vec3 actual = TiltAdapterModelRotation.apply(onModelTop, facing, alongFirst);
                assertEquals(expected.x, actual.x, 1e-6, key);
                assertEquals(expected.y, actual.y, 1e-6, key);
                assertEquals(expected.z, actual.z, 1e-6, key);
            }
        }
    }
}
