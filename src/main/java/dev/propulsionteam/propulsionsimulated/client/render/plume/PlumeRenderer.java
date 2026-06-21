package dev.propulsionteam.propulsionsimulated.client.render.plume;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class PlumeRenderer {
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
        if (params.power() <= 0.004f) return;
        if (params.alpha() <= 0.004f) return;
        if (params.length() <= 0.004f) return;
        if (params.radius() <= 0.004f) return;
        if (direction.lengthSqr() <= 1.0e-8d) return;

        ShaderInstance shader = PlumeShaders.plume();
        if (shader != null) {
            set(shader, "Time", time);
            set(shader, "Power", params.power());
        }

        Vec3 dir = direction.normalize();

        ms.pushPose();
        rotateLocalDownTo(ms, dir);

        VertexConsumer vc = buffer.getBuffer(PlumeRenderType.plume());

        drawPlumeMesh(ms.last().pose(), vc, params, params.length(), params.radius(), 32, 18, params.alpha(), time, 0.0f);
        drawPlumeMesh(ms.last().pose(), vc, params, params.length() * 0.86f, params.radius() * 0.54f, 28, 14, params.alpha() * 0.82f, time, 0.35f);
        drawPlumeMesh(ms.last().pose(), vc, params, params.length() * 0.62f, params.radius() * 0.22f, 24, 10, params.alpha() * 0.74f, time, 0.7f);

        ms.popPose();
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

    private static void drawPlumeMesh(Matrix4f matrix, VertexConsumer vc, PlumeRenderParams params, float length, float radius, int segments, int rings, float alpha, float time, float phase) {
        for (int j = 0; j < rings; j++) {
            float t0 = (float) j / rings;
            float t1 = (float) (j + 1) / rings;

            float y0 = -length * t0;
            float y1 = -length * t1;

            float r0 = radiusAt(params.shape(), radius, t0, time, phase);
            float r1 = radiusAt(params.shape(), radius, t1, time, phase);

            int a0 = alphaAt(alpha, t0);
            int a1 = alphaAt(alpha, t1);

            for (int i = 0; i < segments; i++) {
                float u0 = (float) i / segments;
                float u1 = (float) (i + 1) / segments;

                float[] p00 = point(params.shape(), r0, u0);
                float[] p01 = point(params.shape(), r0, u1);
                float[] p10 = point(params.shape(), r1, u0);
                float[] p11 = point(params.shape(), r1, u1);

                v(vc, matrix, p00[0], y0, p00[1], u0, t0, a0, params);
                v(vc, matrix, p10[0], y1, p10[1], u0, t1, a1, params);
                v(vc, matrix, p11[0], y1, p11[1], u1, t1, a1, params);
                v(vc, matrix, p01[0], y0, p01[1], u1, t0, a0, params);
            }
        }
    }

    private static float[] point(PlumeShape shape, float radius, float u) {
        if (shape == PlumeShape.SQUARE) return squarePoint(radius, u);
        return roundPoint(radius, u);
    }

    private static float[] roundPoint(float radius, float u) {
        float angle = Mth.TWO_PI * u;
        return new float[] {
                Mth.cos(angle) * radius,
                Mth.sin(angle) * radius
        };
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

    private static float radiusAt(PlumeShape shape, float baseRadius, float t, float time, float phase) {
        if (shape == PlumeShape.ROUND) {
            float ionNeedle = 1.0f - smoothstep(0.18f, 1.0f, t) * 0.48f;
            float ionPulse = 1.0f + 0.008f * Mth.sin(time * 48.0f + phase * 13.0f + t * 24.0f);
            return baseRadius * ionNeedle * ionPulse;
        }

        float nearNozzle = Mth.lerp(smoothstep(0.0f, 0.12f, t), 0.58f, 1.0f);
        float expansion = 1.0f + 0.42f * smoothstep(0.10f, 0.55f, t);
        float compression = 1.0f - 0.18f * smoothstep(0.55f, 0.82f, t);
        float tip = 1.0f - smoothstep(0.78f, 1.0f, t) * 0.82f;
        float pulse = 1.0f + 0.012f * Mth.sin(time * 34.0f + phase * 11.0f + t * 18.0f);
        return baseRadius * nearNozzle * expansion * compression * tip * pulse;
    }

    private static int alphaAt(float alpha, float t) {
        float fadeIn = smoothstep(0.0f, 0.045f, t);
        float fadeOut = 1.0f - smoothstep(0.80f, 1.0f, t);
        float body = 1.0f - 0.22f * smoothstep(0.45f, 0.88f, t);
        return Mth.clamp((int) (alpha * 255.0f * fadeIn * fadeOut * body), 0, 255);
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