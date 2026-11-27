package dev.propulsionteam.propulsionsimulated.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.lidar.LidarDebug;
import dev.propulsionteam.propulsionsimulated.debug.PropulsionDebug;
import dev.propulsionteam.propulsionsimulated.debug.routes.MainDebugRoute;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public final class LidarDebugRenderer {
    private static final int LIFETIME_TICKS = 4;
    private static final float MARKER = 0.15f;
    private static final float[][] COLORS = {
        {0.0f, 0.55f, 0.65f},
        {1.0f, 0.25f, 0.25f},
        {1.0f, 0.3f, 1.0f},
        {1.0f, 0.9f, 0.2f},
    };
    private static final Map<Long, Scan> SCANS = new ConcurrentHashMap<>();

    private LidarDebugRenderer() {}

    private static final class Scan {
        final Vec3 origin;
        final float[] ends;
        final byte[] kinds;
        int ticks = LIFETIME_TICKS;

        Scan(Vec3 origin, float[] ends, byte[] kinds) {
            this.origin = origin;
            this.ends = ends;
            this.kinds = kinds;
        }
    }

    public static void show(long lidar, Vec3 origin, float[] ends, byte[] kinds) {
        if (PropulsionDebug.isDebug(MainDebugRoute.LIDAR)) SCANS.put(lidar, new Scan(origin, ends, kinds));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (SCANS.isEmpty() || Minecraft.getInstance().isPaused()) return;
        SCANS.values().removeIf(scan -> --scan.ticks <= 0);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || SCANS.isEmpty()) return;
        if (!PropulsionDebug.isDebug(MainDebugRoute.LIDAR)) {
            SCANS.clear();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer lines = mc.renderBuffers().bufferSource().getBuffer(PropulsionRenderTypes.DEBUG_LINE);

        for (Scan scan : SCANS.values()) {
            float ox = (float) scan.origin.x, oy = (float) scan.origin.y, oz = (float) scan.origin.z;
            for (int i = 0; i < scan.kinds.length; i++) {
                float[] c = COLORS[scan.kinds[i]];
                float x = scan.ends[i * 3], y = scan.ends[i * 3 + 1], z = scan.ends[i * 3 + 2];
                line(lines, matrix, ox, oy, oz, x, y, z, c);
                if (scan.kinds[i] == LidarDebug.MISS) continue;
                line(lines, matrix, x - MARKER, y, z, x + MARKER, y, z, c);
                line(lines, matrix, x, y - MARKER, z, x, y + MARKER, z, c);
                line(lines, matrix, x, y, z - MARKER, x, y, z + MARKER, c);
            }
        }
        poseStack.popPose();
    }

    private static void line(VertexConsumer lines, Matrix4f matrix, float x1, float y1, float z1,
                             float x2, float y2, float z2, float[] c) {
        lines.addVertex(matrix, x1, y1, z1).setColor(c[0], c[1], c[2], 1.0f);
        lines.addVertex(matrix, x2, y2, z2).setColor(c[0], c[1], c[2], 1.0f);
    }
}
