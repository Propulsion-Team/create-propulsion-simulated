package dev.propulsionteam.propulsionsimulated.content.toolgun;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ToolgunNativeHandRenderingTest {
    @Test
    void usesWorldshaperRendererDirectly() {
        assertEquals(ZapperRenderHandler.class, ToolgunRenderHandler.class.getSuperclass());
    }

    @Test
    void anchorsBothPalmsToTheHandleForNormalAndSlimSkins() {
        Matrix4f tool = new Matrix4f().translation(0.4f, -0.3f, -0.8f)
            .rotateXYZ(0.3f, 0.8f, 1.2f).scale(0.8f);
        Matrix4f original = new Matrix4f(tool);
        Vector3f expectedGrip = tool.transformPosition(ToolgunRenderHandler.HANDLE_GRIP
            .add(ToolgunRenderHandler.GRIP_OFFSET.scale(1 / 16.0)).toVector3f());
        for (boolean rightHand : new boolean[] {false, true}) {
            for (boolean slim : new boolean[] {false, true}) {
                Matrix4f hand = ToolgunRenderHandler.handPose(tool, rightHand, slim);
                float palmX = (slim ? 5.5f : 6) / 16;
                Vector3f palm = new Vector3f(rightHand ? -palmX : palmX, 10 / 16f, 0);
                assertVector(expectedGrip, hand.transformPosition(palm));
                assertVector(tool.getScale(new Vector3f()), hand.getScale(new Vector3f()));
                assertEquals(original, tool);
            }
        }
    }

    @Test
    void forearmFollowsTheExportedGripRotation() {
        Matrix4f tool = new Matrix4f().translation(0.4f, -0.3f, -0.8f).rotateXYZ(0.3f, 0.8f, 1.2f);
        Vector3f direction = new Matrix4f(tool).rotateXYZ((float) Math.toRadians(90), 0,
            (float) Math.toRadians(3.5)).transformDirection(new Vector3f(1, 0, 0)).normalize();
        for (boolean rightHand : new boolean[] {false, true}) {
            for (boolean slim : new boolean[] {false, true}) {
                Matrix4f hand = ToolgunRenderHandler.handPose(tool, rightHand, slim);
                Vector3f forearmDirection = hand.transformDirection(new Vector3f(0, 1, 0)).normalize();
                assertVector(direction, forearmDirection);
            }
        }
    }

    @Test
    void handleAnchorMatchesTheRotatedHandleInTheModel() throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream("assets/createpropulsion/models/item/toolgun.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject handle = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements")
                .get(0).getAsJsonObject();
            assertEquals("handle", handle.get("name").getAsString());
            Vector3f center = vector(handle, "from").add(vector(handle, "to")).mul(0.5f);
            JsonObject rotation = handle.getAsJsonObject("rotation");
            Vector3f origin = vector(rotation, "origin");
            new Matrix4f().translation(origin).rotateZ((float) Math.toRadians(rotation.get("angle").getAsFloat()))
                .translate(-origin.x(), -origin.y(), -origin.z()).transformPosition(center);
            assertVector(center.div(16), ToolgunRenderHandler.HANDLE_GRIP.toVector3f());
        }
    }

    @Test
    void preservesTheExistingToolPlacementAndRecoil() {
        for (float flip : new float[] {-1, 1}) {
            PoseStack pose = new PoseStack();
            ToolgunRenderHandler.applyToolPose(pose, flip, 0, 0.25f, 0.1f);
            Matrix4f expected = new Matrix4f().translation(flip * (0.64f - 0.1f),
                -0.4f - 0.25f * 0.6f, -0.72f - 0.1f + 0.1f);
            assertEquals(expected, pose.last().pose());
        }
    }

    @Test
    void appliesExportedItemPlacementForBothHands() {
        TestRenderer renderer = new TestRenderer();
        for (float flip : new float[] {-1, 1}) {
            PoseStack pose = new PoseStack();
            Matrix4f expected = new Matrix4f().translate(flip * -0.1f, 0.1f, -0.4f)
                .rotateY((float) Math.toRadians(flip * 5)).translate(0, 1.3f / 16, -1.3f / 16);
            renderer.applyToolTransform(pose, flip);
            for (int column = 0; column < 4; column++) {
                for (int row = 0; row < 4; row++) {
                    assertEquals(expected.get(column, row), pose.last().pose().get(column, row), 1e-6);
                }
            }
        }
    }

    @Test
    void steveArmMatricesMatchTheDesktopExportForBothHands() throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream("toolgun-grip.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject exported = JsonParser.parseReader(reader).getAsJsonObject();
            for (boolean rightHand : new boolean[] {false, true}) {
                var expected = exported.getAsJsonArray(rightHand ? "right_arm_from_model" : "left_arm_from_model");
                Matrix4f actual = ToolgunRenderHandler.handPose(new Matrix4f(), rightHand, false);
                for (int row = 0; row < 4; row++) {
                    for (int column = 0; column < 4; column++) {
                        assertEquals(expected.get(row).getAsJsonArray().get(column).getAsDouble(),
                            actual.get(column, row), 1e-6);
                    }
                }
            }
        }
    }

    private static void assertVector(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x(), actual.x(), 1e-6);
        assertEquals(expected.y(), actual.y(), 1e-6);
        assertEquals(expected.z(), actual.z(), 1e-6);
    }

    private static Vector3f vector(JsonObject object, String key) {
        var values = object.getAsJsonArray(key);
        return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    private static class TestRenderer extends ToolgunRenderHandler {
        void applyToolTransform(PoseStack pose, float flip) {
            transformTool(pose, flip, 0.25f, 0.1f, 0.5f);
        }
    }
}
