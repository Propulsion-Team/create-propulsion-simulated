package dev.propulsionteam.propulsionsimulated.content.toolgun;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolgunLaserHandlerTest {
    @Test
    void shootingOriginUsesWorldshaperBarrelOffset() {
        assertDirection(ToolgunLaserHandler.BARREL_OFFSET, 0.35f, -0.1f, 1);
    }

    @Test
    void firstPersonPlacementMatchesWorldshaperWithToolgunAxisAlignment() throws IOException {
        for (boolean leftHand : new boolean[] {false, true}) {
            float flip = leftHand ? -1 : 1;
            Matrix4f expected = new Matrix4f().translation(flip / 16f, 4 / 16f, 1 / 16f)
                .rotateXYZ(radians(10), 0, radians(flip * 10)).rotateY(radians(90));
            assertMatrix(expected, displayTransform("firstperson_" + (leftHand ? "lefthand" : "righthand"), leftHand));
        }
    }

    @Test
    void thirdPersonPlacementMatchesWorldshaperWithToolgunAxisAlignment() throws IOException {
        Matrix4f expected = new Matrix4f().translation(0, 4 / 16f, -2.5f / 16)
            .rotateXYZ(radians(1), radians(90), 0).scale(0.8f);
        assertMatrix(expected, displayTransform("thirdperson_righthand", false));
        assertMatrix(expected, displayTransform("thirdperson_lefthand", true));
    }

    @Test
    void ordinaryWorldHitsDoNotChangeAim() {
        Vec3 direction = ToolgunLaserHandler.shotDirection(new Vec3(10, 64, 10),
            new Vec3(10, 64, 0), position -> position);
        assertDirection(direction, 0, 0, -1);
    }

    @Test
    void sublevelStorageCoordinatesAreProjectedBeforeAiming() {
        Vec3 direction = ToolgunLaserHandler.shotDirection(new Vec3(10, 64, 10),
            new Vec3(30000010, 64, 0), position -> position.add(-30000000, 0, 0));
        assertDirection(direction, 0, 0, -1);
    }

    @Test
    void rotatedSublevelHitsAimAtTheirWorldPosition() {
        Matrix4d pose = new Matrix4d().rotateY(Math.PI / 2).translate(-30000000, 0, 0);
        Vec3 direction = ToolgunLaserHandler.shotDirection(new Vec3(0, 64, 0),
            new Vec3(30000010, 64, 0), position -> {
                Vector3d world = pose.transformPosition(new Vector3d(position.x, position.y, position.z));
                return new Vec3(world.x(), world.y(), world.z());
            });
        assertDirection(direction, 0, 0, -1);
    }

    @Test
    void outlineStartsFadingImmediatelyAfterImpact() {
        assertEquals(1, ToolgunLaserHandler.outlineAlpha(0));
        assertEquals(0.9f, ToolgunLaserHandler.outlineAlpha(1));
        assertEquals(0.75f, ToolgunLaserHandler.outlineAlpha(2.5));
    }

    @Test
    void outlineDisappearsHalfASecondAfterImpact() {
        assertEquals(0.5f, ToolgunLaserHandler.outlineAlpha(5));
        assertEquals(0.25f, ToolgunLaserHandler.outlineAlpha(7.5));
        assertEquals(0, ToolgunLaserHandler.outlineAlpha(10));
        assertEquals(0, ToolgunLaserHandler.outlineAlpha(20));
    }

    private static void assertDirection(Vec3 actual, double x, double y, double z) {
        assertEquals(x, actual.x, 1e-8);
        assertEquals(y, actual.y, 1e-8);
        assertEquals(z, actual.z, 1e-8);
    }

    private static Matrix4f displayTransform(String context, boolean leftHand) throws IOException {
        var stream = ToolgunLaserHandlerTest.class.getClassLoader()
            .getResourceAsStream("assets/createpropulsion/models/item/toolgun.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject transform = JsonParser.parseReader(reader).getAsJsonObject()
                .getAsJsonObject("display").getAsJsonObject(context);
            JsonArray translation = transform.getAsJsonArray("translation");
            JsonArray rotation = transform.getAsJsonArray("rotation");
            float flip = leftHand ? -1 : 1;
            Matrix4f matrix = new Matrix4f().translation(flip * translation.get(0).getAsFloat() / 16,
                translation.get(1).getAsFloat() / 16, translation.get(2).getAsFloat() / 16)
                .rotateXYZ(radians(rotation.get(0).getAsFloat()), radians(flip * rotation.get(1).getAsFloat()),
                    radians(flip * rotation.get(2).getAsFloat()));
            if (transform.has("scale")) {
                JsonArray scale = transform.getAsJsonArray("scale");
                matrix.scale(scale.get(0).getAsFloat(), scale.get(1).getAsFloat(), scale.get(2).getAsFloat());
            }
            return matrix;
        }
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static void assertMatrix(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), 1e-5);
            }
        }
    }
}
