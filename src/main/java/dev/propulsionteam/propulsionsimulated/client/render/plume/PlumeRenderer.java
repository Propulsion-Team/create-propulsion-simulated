package dev.propulsionteam.propulsionsimulated.client.render.plume;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public final class PlumeRenderer {
    private static final List<PendingPlume> PENDING = new ArrayList<>();

    private record PendingPlume(Matrix4f pose, Matrix4f modelView, Matrix4f projection,
                                PlumeRenderParams params, float time, Vec3 direction, Quaternionf orientation) {}

    private PlumeRenderer() {
    }

    public static void render(PoseStack ms, MultiBufferSource buffer, PlumeRenderParams params, float time) {
        Vec3 direction = new Vec3(
                params.direction().getStepX(),
                params.direction().getStepY(),
                params.direction().getStepZ()
        );
        render(ms, buffer, params, time, direction);
    }

    public static void render(PoseStack ms, MultiBufferSource buffer, PlumeRenderParams params, float time, Vec3 direction) {
        render(ms, buffer, params, time, direction, null);
    }

    public static void render(PoseStack ms, MultiBufferSource buffer, PlumeRenderParams params, float time, Vec3 direction, Quaternionf orientation) {
        if (params.power() <= 0.004f) return;
        if (params.alpha() <= 0.004f) return;
        if (params.length() <= 0.004f) return;
        if (params.radius() <= 0.004f) return;
        if (direction.lengthSqr() <= 1.0e-8d) return;

        PENDING.add(new PendingPlume(new Matrix4f(ms.last().pose()),
                new Matrix4f(RenderSystem.getModelViewMatrix()), new Matrix4f(RenderSystem.getProjectionMatrix()),
                params, time, direction, orientation));
    }

    @SubscribeEvent
    public static void renderPending(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            PENDING.clear();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (PENDING.isEmpty()) return;
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        PoseStack ms = new PoseStack();
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            for (PendingPlume plume : PENDING) {
                modelView.set(plume.modelView());
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(plume.projection(), sorting);
                ms.last().pose().set(plume.pose());
                draw(ms, buffer, plume.params(), plume.time(), plume.direction(), plume.orientation());
                buffer.endBatch(PlumeRenderType.plume());
            }
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            PENDING.clear();
        }
    }

    private static void draw(PoseStack ms, MultiBufferSource buffer, PlumeRenderParams params, float time, Vec3 direction, Quaternionf orientation) {
        ShaderInstance shader = PlumeShaders.plume();
        if (shader != null) {
            set(shader, "Time", time);
            set(shader, "Power", params.power());
            set(shader, "Voxel", isBoxes(params.shape()) ? params.length() * 16.0f : 0.0f);
            var dye = shader.getUniform("DyeColor");
            if (dye != null) {
                Integer color = params.dyeColor();
                if (color == null) {
                    dye.set(-1.0f, -1.0f, -1.0f);
                } else {
                    dye.set(((color >> 16) & 0xFF) / 255.0f,
                            ((color >> 8) & 0xFF) / 255.0f, (color & 0xFF) / 255.0f);
                }
            }
        }

        Vec3 dir = direction.normalize();

        ms.pushPose();
        if (orientation != null) {
            ms.mulPose(orientation);
        } else {
            rotateLocalDownTo(ms, dir);
        }

        VertexConsumer vc = buffer.getBuffer(PlumeRenderType.plume());

        if (isBoxes(params.shape())) {
            drawBoxes(ms, vc, params, time);
        }

        ms.popPose();
    }

    private static final float[] FLAME_WIDTHS = {1.0f, 0.786f, 0.571f};
    private static final float[] FLAME_LENGTHS = {0.517f, 0.759f, 1.0f};
    private static final float[] PLASMA_WIDTHS = {1.0f, 0.54f, 0.22f};
    private static final float[] PLASMA_LENGTHS = {1.0f, 0.86f, 0.62f};
    private static final float[] BOX_HEAT = {1.0f, 0.75f, 0.5f};
    private static final float[] BOX_ALPHA = {0.75f, 0.85f, 0.85f};
    private static final float[] LAYER_START = {0.0f, 0.1f, 0.25f};
    private static final float[] LAYER_END = {0.15f, 0.4f, 0.7f};
    private static final float[] BOX_FREQ ={0.83f, 1.07f, 1.31f};
    private static final float[] BOX_PHASE = {0.0f, 2.1f, 4.4f};

    private static boolean isBoxes(PlumeShape shape) {
        return shape == PlumeShape.FLAME_BOXES || shape == PlumeShape.PLASMA_BOXES;
    }

    private static void drawBoxes(PoseStack ms, VertexConsumer vc, PlumeRenderParams params, float time) {
        boolean plasma = params.shape() == PlumeShape.PLASMA_BOXES;
        float[] widths = plasma ? PLASMA_WIDTHS : FLAME_WIDTHS;
        float[] lengths = plasma ? PLASMA_LENGTHS : FLAME_LENGTHS;
        float wobble = plasma || params.red() < 0.5f ? 0.4f : 1.0f;
        float power = params.power();
        float settle = params.settle();
        float flare = params.flare();
        float diameter = params.radius() * 32.0f * (0.35f + 0.65f * smoothstep(0.0f, 0.6f, power));
        float lengthScale = 0.2f + 0.8f * smoothstep(0.0f, 0.3f, power);
        float fadeAll = smoothstep(0.0f, 0.15f, power);
        float tick = time * 20.0f;

        for (int i = 0; i < 3; i++) {
            float exactVoxels = diameter * widths[i];
            float voxels = Math.max(1.0f, Mth.lerp(settle, exactVoxels, 2.0f * Math.round(exactVoxels / 2.0f)));
            float half = voxels / 32.0f - 0.004f;
            float stretch = wobble * (1.5f * Mth.sin(tick * BOX_FREQ[i] + BOX_PHASE[i])
                    + 0.5f * Mth.sin(tick * BOX_FREQ[i] * 2.3f + BOX_PHASE[i] * 1.7f)) / 16.0f;
            float exactLength = Math.max(1.0f / 16.0f, params.length() * lengths[i] * lengthScale + stretch);
            float length = Mth.lerp(settle, exactLength, Math.max(1.0f, Math.round(exactLength * 16.0f)) / 16.0f);
            float layerFade = smoothstep(LAYER_START[i], LAYER_END[i], power);
            float layerAlpha = params.alpha() * BOX_ALPHA[i] * 255.0f * layerFade * fadeAll;
            int alpha = Mth.clamp((int) layerAlpha, 0, 255);

            ms.pushPose();
            if (plasma && i == 1) ms.mulPose(Axis.YP.rotationDegrees(45.0f));
            Matrix4f m = ms.last().pose();
            float vEnd = BOX_HEAT[i];
            float tipHalf = plasma ? half * Mth.lerp(flare, 0.25f, 1.0f) : half;
            for (int side = 0; side < 4; side++) {
                float[] a = squarePoint(half, side / 4.0f);
                float[] b = squarePoint(half, (side + 1) / 4.0f);
                float[] c = squarePoint(tipHalf, (side + 1) / 4.0f);
                float[] d = squarePoint(tipHalf, side / 4.0f);
                float u0 = side / 4.0f;
                float u1 = (side + 1) / 4.0f;
                v(vc, m, a[0], 0.0f, a[1], u0, 0.0f, alpha, params);
                v(vc, m, b[0], 0.0f, b[1], u1, 0.0f, alpha, params);
                v(vc, m, c[0], -length, c[1], u1, vEnd, 0, params);
                v(vc, m, d[0], -length, d[1], u0, vEnd, 0, params);
            }
            if (i == 0) {
                int capAlpha = Mth.clamp((int) (params.alpha() * 255.0f * fadeAll), 0, 255);
                float capY = -0.002f;
                float capHalf = params.capRadius() > 0.0f ? params.capRadius() : half;
                v(vc, m, -capHalf, capY, -capHalf, 0.0f, 0.0f, capAlpha, params);
                v(vc, m, -capHalf, capY, capHalf, 0.25f, 0.0f, capAlpha, params);
                v(vc, m, capHalf, capY, capHalf, 0.5f, 0.0f, capAlpha, params);
                v(vc, m, capHalf, capY, -capHalf, 0.75f, 0.0f, capAlpha, params);
            }
            ms.popPose();
        }
    }

    private static void rotateLocalDownTo(PoseStack ms, Direction direction) {
        switch (direction) {
            case DOWN -> {
            }
            case UP -> ms.mulPose(Axis.XP.rotationDegrees(180.0f));
            case NORTH -> ms.mulPose(Axis.XP.rotationDegrees(90.0f));
            case SOUTH -> ms.mulPose(Axis.XP.rotationDegrees(-90.0f));
            case WEST -> ms.mulPose(Axis.ZP.rotationDegrees(-90.0f));
            case EAST -> ms.mulPose(Axis.ZP.rotationDegrees(90.0f));
        }
    }

    private static void rotateLocalDownTo(PoseStack ms, Vec3 direction) {
        Quaternionf q = new Quaternionf().rotationTo(
                0.0f,
                -1.0f,
                0.0f,
                (float) direction.x,
                (float) direction.y,
                (float) direction.z
        );
        ms.mulPose(q);
    }

    private static float[] squarePoint(float radius, float u) {
        float p = (u - Mth.floor(u)) * 4.0f;

        if (p < 1.0f) {
            return new float[] {
                    Mth.lerp(p, radius, -radius),
                    -radius
            };
        }

        if (p < 2.0f) {
            return new float[] {
                    -radius,
                    Mth.lerp(p - 1.0f, -radius, radius)
            };
        }

        if (p < 3.0f) {
            return new float[] {
                    Mth.lerp(p - 2.0f, -radius, radius),
                    radius
            };
        }

        return new float[] {
                radius,
                Mth.lerp(p - 3.0f, radius, -radius)
        };
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static void v(VertexConsumer vc, Matrix4f matrix, float x, float y, float z, float u, float v, int alpha, PlumeRenderParams params) {
        vc.addVertex(matrix, x, y, z)
                .setUv(u, v)
                .setColor(
                        Mth.clamp((int) (params.red() * 255.0f), 0, 255),
                        Mth.clamp((int) (params.green() * 255.0f), 0, 255),
                        Mth.clamp((int) (params.blue() * 255.0f), 0, 255),
                        alpha
                );
    }

    private static void set(ShaderInstance shader, String name, float value) {
        var uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

}
