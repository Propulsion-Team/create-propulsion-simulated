package dev.propulsionteam.propulsionsimulated.content.toolgun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolgunNativeHandRenderingTest {
    @Test
    void usesWorldshaperRendererDirectly() {
        assertEquals(ZapperRenderHandler.class, ToolgunRenderHandler.class.getSuperclass());
    }

    @Test
    void preservesSharedWorldshaperAndHotGlueGunArmPoseForBothHands() {
        TestRenderer renderer = new TestRenderer();
        for (float flip : new float[] {-1, 1}) {
            PoseStack pose = new PoseStack();
            pose.mulPose(new Matrix4f().translation(flip * 0.4f, -0.3f, -0.8f)
                .rotateXYZ(0.3f, flip * 0.8f, 1.2f));
            Matrix4f expected = new Matrix4f(pose.last().pose());
            renderer.applyHandTransform(pose, flip);
            for (int column = 0; column < 4; column++) {
                for (int row = 0; row < 4; row++) {
                    assertEquals(expected.get(column, row), pose.last().pose().get(column, row));
                }
            }
        }
    }

    @Test
    void movesOnlyTheToolOneVoxelUpAndBackForBothHands() {
        TestRenderer renderer = new TestRenderer();
        for (float flip : new float[] {-1, 1}) {
            PoseStack pose = new PoseStack();
            Matrix4f expected = new Matrix4f().translate(0, 1 / 16f, 1 / 16f)
                .translate(flip * -0.1f, 0.1f, -0.4f).rotateY((float) Math.toRadians(flip * 5));
            renderer.applyToolTransform(pose, flip);
            for (int column = 0; column < 4; column++) {
                for (int row = 0; row < 4; row++) {
                    assertEquals(expected.get(column, row), pose.last().pose().get(column, row), 1e-6);
                }
            }
        }
    }

    private static class TestRenderer extends ToolgunRenderHandler {
        void applyHandTransform(PoseStack pose, float flip) {
            transformHand(pose, flip, 0.25f, 0.1f, 0.5f);
        }

        void applyToolTransform(PoseStack pose, float flip) {
            transformTool(pose, flip, 0.25f, 0.1f, 0.5f);
        }
    }
}
