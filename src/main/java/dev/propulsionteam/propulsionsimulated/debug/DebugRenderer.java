package dev.propulsionteam.propulsionsimulated.debug;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public class DebugRenderer {
    private static class TimedBoxData {
        final Vec3 position;
        final Vec3 size;
        final Quaternionf rotation;
        final Color color;
        final boolean onlyInDebugMode;
        int remainingTicks;

        TimedBoxData(Level level, Vec3 position, Vec3 size, Quaternionf rotation, Color color, boolean onlyInDebugMode, int initialTicks) {
            this.position = position;
            this.size = size;
            this.rotation = rotation;
            this.color = color;
            this.onlyInDebugMode = onlyInDebugMode;
            this.remainingTicks = initialTicks;
        }
    }

    private static final Map<String, TimedBoxData> timedBoxes = new ConcurrentHashMap<>();

    //Full
    public static void drawBox(String identifier, Vec3 center, Vec3 size, Quaternionf rotation, Color color, boolean onlyInDebugMode, int ticksToRender) {
        if (identifier == null || identifier.isEmpty()) {
            System.err.println("[DebugRenderer] Error: Null or empty identifier provided for drawBox.");
            return;
        }
        if (ticksToRender <= 0) {
            timedBoxes.remove(identifier);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return; //In case we do not have a level yet for some reason

        TimedBoxData data = new TimedBoxData(level, center, size, rotation, color, onlyInDebugMode, ticksToRender);
        timedBoxes.put(identifier, data);
    }

    //Basic
    public static void drawBox(String identifier, Vec3 center, Vec3 size, int ticksToRender) {
        drawBox(identifier, center, size, new Quaternionf(), Color.WHITE, false, ticksToRender);
    }

    public static void drawBox(String identifier, Vec3 center, Vec3 size, Color color, int ticksToRender) {
        drawBox(identifier, center, size, new Quaternionf(), color, false, ticksToRender);
    }
    //AABB
    public static void drawBox(String identifier, AABB aabb, int ticksToRender) {
        drawBox(identifier, aabb, Color.WHITE, ticksToRender);
    }

    public static void drawBox(String identifier, AABB aabb, Color color, int ticksToRender) {
        if (aabb == null) {
            System.err.println("[DebugRenderer] Error: Null AABB provided for drawBox with identifier: " + identifier);
            removeBox(identifier);
            return;
        }
        Vec3 center = aabb.getCenter();
        Vec3 size = new Vec3(aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
        drawBox(identifier, center, size, new Quaternionf(), color, false, ticksToRender);
    }

    //BlockPos
    public static void drawBox(String identifier, BlockPos blockPos, int ticksToRender) {
        drawBox(identifier, blockPos, Color.WHITE, ticksToRender);
    }

    public static void drawBox(String identifier, BlockPos blockPos, Color color, int ticksToRender) {
         if (blockPos == null) {
            System.err.println("[DebugRenderer] Error: Null BlockPos provided for drawBox with identifier: " + identifier);
            removeBox(identifier);
            return;
        }
        Vec3 center = blockPos.getCenter();
        Vec3 size = new Vec3(0.999, 0.999, 0.999); //0.999 to avoid z-figting
        drawBox(identifier, center, size, new Quaternionf(), color, false, ticksToRender);
    }

    //This is my new way of rendering debug arrows :P
    public static void drawElongatedBox(String identifier, Vec3 posA, Vec3 posB, float thickness, Color color, boolean onlyInDebugMode, int ticksToRender) {
        Vec3 center = posA.add(posB).scale(0.5);
        Vec3 direction = posB.subtract(posA);
        double length = direction.length();
        Vec3 size = new Vec3(thickness, thickness, length);
        Vector3f dir = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z).normalize();
        Quaternionf rotation = getRotationFromZ(dir);
        drawBox(identifier, center, size, rotation, color, onlyInDebugMode, ticksToRender);
    }

    public static void removeBox(String identifier) {
        if (identifier != null) {
            timedBoxes.remove(identifier);
        }
    }

    //Decay boxes
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (timedBoxes.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && !mc.isPaused()) {
            timedBoxes.entrySet().removeIf(entry -> {
                TimedBoxData data = entry.getValue();
                data.remainingTicks--;
                return data.remainingTicks <= 0;
            });
        }
    }

    //Render boxes
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || timedBoxes.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null || mc.renderBuffers() == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(PropulsionRenderTypes.DEBUG_LINE);
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        boolean isDebugMode = mc.getDebugOverlay().showDebugScreen();

        for (TimedBoxData boxData : timedBoxes.values()) {
            if (boxData.onlyInDebugMode && !isDebugMode) {
                continue;
            }

            renderWireBox(poseStack, vertexConsumer, boxData);
        }

        poseStack.popPose();
    }

    private static void renderWireBox(PoseStack poseStack, VertexConsumer vertexConsumer, TimedBoxData data) {
        poseStack.pushPose();

        poseStack.translate(data.position.x, data.position.y, data.position.z);
        poseStack.mulPose(data.rotation);

        Matrix4f matrix = poseStack.last().pose();

        float halfW = (float) data.size.x / 2.0f;
        float halfH = (float) data.size.y / 2.0f;
        float halfD = (float) data.size.z / 2.0f;

        Vector3f p0 = new Vector3f(-halfW, -halfH, -halfD);
        Vector3f p1 = new Vector3f( halfW, -halfH, -halfD);
        Vector3f p2 = new Vector3f( halfW, -halfH,  halfD);
        Vector3f p3 = new Vector3f(-halfW, -halfH,  halfD);
        Vector3f p4 = new Vector3f(-halfW,  halfH, -halfD);
        Vector3f p5 = new Vector3f( halfW,  halfH, -halfD);
        Vector3f p6 = new Vector3f( halfW,  halfH,  halfD);
        Vector3f p7 = new Vector3f(-halfW,  halfH,  halfD);

        float r = data.color.getRed() / 255.0f;
        float g = data.color.getGreen() / 255.0f;
        float b = data.color.getBlue() / 255.0f;
        float a = data.color.getAlpha() / 255.0f;

        drawLine(vertexConsumer, matrix, p0, p1, r, g, b, a);
        drawLine(vertexConsumer, matrix, p1, p2, r, g, b, a);
        drawLine(vertexConsumer, matrix, p2, p3, r, g, b, a);
        drawLine(vertexConsumer, matrix, p3, p0, r, g, b, a);
        drawLine(vertexConsumer, matrix, p4, p5, r, g, b, a);
        drawLine(vertexConsumer, matrix, p5, p6, r, g, b, a);
        drawLine(vertexConsumer, matrix, p6, p7, r, g, b, a);
        drawLine(vertexConsumer, matrix, p7, p4, r, g, b, a);
        drawLine(vertexConsumer, matrix, p0, p4, r, g, b, a);
        drawLine(vertexConsumer, matrix, p1, p5, r, g, b, a);
        drawLine(vertexConsumer, matrix, p2, p6, r, g, b, a);
        drawLine(vertexConsumer, matrix, p3, p7, r, g, b, a);

        poseStack.popPose();
    }

    private static void drawLine(VertexConsumer consumer, Matrix4f matrix, Vector3f pos1, Vector3f pos2, float r, float g, float b, float a) {
        consumer.addVertex(matrix, pos1.x(), pos1.y(), pos1.z()).setColor(r, g, b, a);
        consumer.addVertex(matrix, pos2.x(), pos2.y(), pos2.z()).setColor(r, g, b, a);
    }

    //Utility

    private static Quaternionf getRotationFromZ(Vector3f direction) {
        Vector3f zAxis = new Vector3f(0, 0, 1);
        Vector3f axis = new Vector3f();
        zAxis.cross(direction, axis);

        float dot = zAxis.dot(direction);
        float angle = (float) Math.acos(dot);

        if (axis.lengthSquared() < 1e-6) {
            //If direction is same or opposite of Z, avoid instability
            if (dot > 0.9999f) {
                return new Quaternionf();
            } else {
                return new Quaternionf().rotateXYZ((float) Math.PI, 0, 0);
            }
        }

        axis.normalize();
        return new Quaternionf().rotateAxis(angle, axis.x, axis.y, axis.z);
    }
}
