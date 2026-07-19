package dev.propulsionteam.propulsionsimulated.client.render.plume;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.PlumeTrailMath;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterPlumeSpec;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** A short adaptive world-space core that hides 20 Hz particle aliasing during rapid motion. */
public final class AdaptivePlumeTrailRenderer {
    private static final Map<AbstractThrusterBlockEntity, TrailState> TRAILS = new WeakHashMap<>();
    private static final double SAMPLE_ANGLE_DEGREES = 1.5d;
    private static final double SAMPLE_DISTANCE_BLOCKS = 0.15d;

    private AdaptivePlumeTrailRenderer() {}

    public static void render(AbstractThrusterBlockEntity be, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffer, ThrusterPlumeSpec spec) {
        if (be == null || be.getLevel() == null || be.isRemoved() || !be.isController()) return;
        TrailState state = TRAILS.computeIfAbsent(be, ignored -> new TrailState());
        double now = be.getLevel().getGameTime() + partialTicks;

        if (spec.active()) {
            state.style = spec.style();
            state.power = spec.power();
            sample(be, state, now);
        } else {
            state.active = false;
            state.quietSamples = 4;
        }

        state.nodes.removeIf(node -> !PlumeTrailMath.isTrailNodeAlive(node.birthTime, now));
        float target = state.active ? Math.max(state.targetCoverage, 0.35f) : 0.0f;
        double delta = state.lastRenderTime < 0.0d ? 0.0d : Math.min(1.0d, now - state.lastRenderTime);
        state.lastRenderTime = now;
        float response = target > state.coverage ? 0.34f : 0.15f;
        state.coverage += (target - state.coverage) * Math.min(1.0f, response * (float) Math.max(1.0d, delta));

        if (state.coverage < 0.01f || state.nodes.size() < 2) {
            if (!spec.active() && state.nodes.isEmpty()) TRAILS.remove(be);
            return;
        }

        Vec3 nozzleWorld = state.lastPosition;
        Vec3 cameraWorld = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = cameraWorld.distanceTo(nozzleWorld);
        if (distance > 128.0d) return;
        int maxNodes = distance <= 64.0d ? PlumeTrailMath.MAX_TRAIL_NODES : 16;
        boolean renderInner = distance <= 64.0d;
        draw(be, state, now, cameraWorld, poseStack, buffer, maxNodes, renderInner);
    }

    private static void sample(AbstractThrusterBlockEntity be, TrailState state, double now) {
        Vec3 localNozzle = be.getParticleDebugNozzlePositionLocal();
        Vec3 worldPosition = Sable.HELPER.projectOutOfSubLevel(be.getLevel(), localNozzle);
        Vec3 localDirection = be.getParticleDebugExhaustDirectionLocal().normalize();
        Vec3 worldAhead = Sable.HELPER.projectOutOfSubLevel(be.getLevel(), localNozzle.add(localDirection));
        Vec3 worldDirection = worldAhead.subtract(worldPosition).normalize();
        Vec3 velocity = state.lastPosition == null ? Vec3.ZERO : worldPosition.subtract(state.lastPosition);
        try {
            Vec3 sampled = Sable.HELPER.getVelocity(be.getLevel(), localNozzle).scale(1.0d / 20.0d);
            if (finite(sampled)) velocity = sampled;
        } catch (RuntimeException ignored) {
        }

        if (state.lastPosition == null || state.lastDirection == null) {
            state.add(new TrailNode(now, worldPosition, velocity, worldDirection,
                    be.getParticleTrailInitialStep(), friction(state.style)));
            state.lastPosition = worldPosition;
            state.lastDirection = worldDirection;
            state.lastVelocity = velocity;
            state.lastSampleTime = now;
            return;
        }

        double angle = PlumeTrailMath.angleDegrees(state.lastDirection, worldDirection);
        double distance = state.lastPosition.distanceTo(worldPosition);
        if (angle < SAMPLE_ANGLE_DEGREES && distance < SAMPLE_DISTANCE_BLOCKS) return;

        double reconstructedGap = worldPosition.distanceTo(state.lastPosition.add(state.lastVelocity));
        float activation = PlumeTrailMath.activationTarget(angle, reconstructedGap, state.active);
        if (activation > 0.0f) {
            state.active = true;
            state.quietSamples = 0;
            state.targetCoverage = activation;
        } else if (state.active && ++state.quietSamples >= 4) {
            state.active = false;
            state.quietSamples = 0;
            state.targetCoverage = 0.0f;
        }

        int steps = PlumeTrailMath.interpolationSteps(angle, distance);
        double startTime = state.lastSampleTime;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = PlumeTrailMath.hermite(state.lastPosition, state.lastVelocity,
                    worldPosition, velocity, t);
            Vec3 d = PlumeTrailMath.slerpDirection(state.lastDirection, worldDirection, t);
            Vec3 v = state.lastVelocity.lerp(velocity, t);
            double birth = Mth.lerp(t, startTime, now);
            state.add(new TrailNode(birth, p, v, d, be.getParticleTrailInitialStep(), friction(state.style)));
        }
        state.lastPosition = worldPosition;
        state.lastDirection = worldDirection;
        state.lastVelocity = velocity;
        state.lastSampleTime = now;
    }

    private static void draw(AbstractThrusterBlockEntity be, TrailState state, double now, Vec3 cameraWorld,
                             PoseStack poseStack, MultiBufferSource buffer, int maxNodes, boolean renderInner) {
        int from = Math.max(0, state.nodes.size() - maxNodes);
        List<TrailPoint> points = new ArrayList<>(state.nodes.size() - from);
        for (int i = from; i < state.nodes.size(); i++) {
            TrailNode node = state.nodes.get(i);
            double age = Math.max(0.0d, now - node.birthTime);
            double exhaustDistance = PlumeTrailMath.integratedDragDistance(node.initialExhaustStep, node.friction, age);
            Vec3 world = node.emissionPosition.add(node.inheritedVelocity.scale(age))
                    .add(node.direction.scale(exhaustDistance));
            float life = 1.0f - Mth.clamp((float) (age / PlumeTrailMath.TRAIL_LIFETIME_TICKS), 0.0f, 1.0f);
            points.add(new TrailPoint(toRenderLocal(be, world), life));
        }
        if (points.size() < 2) return;

        Vec3 cameraLocal = toRenderLocal(be, cameraWorld);
        Appearance appearance = appearance(state.style, state.power);
        VertexConsumer consumer = buffer.getBuffer(PlumeRenderType.plume());
        Matrix4f matrix = poseStack.last().pose();
        drawStrip(matrix, consumer, points, cameraLocal, appearance.radius,
                appearance.r, appearance.g, appearance.b, appearance.alpha * state.coverage);
        if (renderInner) {
            drawStrip(matrix, consumer, points, cameraLocal, appearance.radius * 0.42f,
                    Math.min(1.0f, appearance.r * 1.12f), Math.min(1.0f, appearance.g * 1.18f),
                    Math.min(1.0f, appearance.b * 1.25f), appearance.alpha * state.coverage * 0.78f);
        }
    }

    private static void drawStrip(Matrix4f matrix, VertexConsumer consumer, List<TrailPoint> points, Vec3 camera,
                                  float width, float r, float g, float b, float alpha) {
        Vec3 previousSide = null;
        for (int i = 0; i < points.size() - 1; i++) {
            TrailPoint a = points.get(i);
            TrailPoint c = points.get(i + 1);
            Vec3 tangent = c.position.subtract(a.position).normalize();
            Vec3 midpoint = a.position.add(c.position).scale(0.5d);
            Vec3 side = tangent.cross(camera.subtract(midpoint)).normalize();
            if (side.lengthSqr() < 1.0e-8d) side = previousSide == null ? new Vec3(1, 0, 0) : previousSide;
            if (previousSide != null && side.dot(previousSide) < 0.0d) side = side.scale(-1.0d);
            previousSide = side;

            float endTaperA = endpointTaper(i, points.size());
            float endTaperB = endpointTaper(i + 1, points.size());
            Vec3 sideA = side.scale(width * a.life * endTaperA);
            Vec3 sideB = side.scale(width * c.life * endTaperB);
            int alphaA = Mth.clamp((int) (255.0f * alpha * a.life * endTaperA), 0, 255);
            int alphaB = Mth.clamp((int) (255.0f * alpha * c.life * endTaperB), 0, 255);
            vertex(consumer, matrix, a.position.add(sideA), 0.0f, i, r, g, b, alphaA);
            vertex(consumer, matrix, c.position.add(sideB), 0.0f, i + 1, r, g, b, alphaB);
            vertex(consumer, matrix, c.position.subtract(sideB), 1.0f, i + 1, r, g, b, alphaB);
            vertex(consumer, matrix, a.position.subtract(sideA), 1.0f, i, r, g, b, alphaA);
        }
    }

    private static float endpointTaper(int index, int size) {
        float head = Mth.clamp(index / 2.0f, 0.0f, 1.0f);
        float tail = Mth.clamp((size - 1 - index) / 2.0f, 0.0f, 1.0f);
        return Math.max(0.08f, Math.min(head, tail));
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 p, float u, float v,
                               float r, float g, float b, int alpha) {
        consumer.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .setUv(u, v * 0.2f)
                .setColor((int) (r * 255), (int) (g * 255), (int) (b * 255), alpha);
    }

    private static Vec3 toRenderLocal(AbstractThrusterBlockEntity be, Vec3 world) {
        SubLevel subLevel = Sable.HELPER.getContaining(be.getLevel(), be.getBlockPos());
        if (subLevel != null) {
            Vector3d local = new Vector3d(world.x, world.y, world.z);
            subLevel.logicalPose().transformPositionInverse(local);
            return new Vec3(local.x - be.getBlockPos().getX(), local.y - be.getBlockPos().getY(),
                    local.z - be.getBlockPos().getZ());
        }
        return world.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private static float friction(ThrusterPlumeSpec.Style style) {
        return style == ThrusterPlumeSpec.Style.ION || style == ThrusterPlumeSpec.Style.VECTOR ? 0.995f : 0.99f;
    }

    private static Appearance appearance(ThrusterPlumeSpec.Style style, float power) {
        float radius = (0.15f + power * 0.14f);
        return switch (style) {
            case ION, VECTOR -> new Appearance(radius * 0.72f, 0.28f, 0.68f, 1.0f, 0.68f);
            case PLASMA -> new Appearance(radius, 0.66f, 0.25f, 1.0f, 0.62f);
            case SOLID -> new Appearance(radius * 0.82f, 1.0f, 0.48f, 0.08f, 0.58f);
            case SUPERHEATED_SOLID -> new Appearance(radius, 0.42f, 0.72f, 1.0f, 0.68f);
            case FIRE -> new Appearance(radius, 1.0f, 0.38f, 0.045f, 0.62f);
        };
    }

    private record TrailNode(double birthTime, Vec3 emissionPosition, Vec3 inheritedVelocity,
                             Vec3 direction, double initialExhaustStep, double friction) {}
    private record TrailPoint(Vec3 position, float life) {}
    private record Appearance(float radius, float r, float g, float b, float alpha) {}

    private static final class TrailState {
        final List<TrailNode> nodes = new ArrayList<>();
        Vec3 lastPosition;
        Vec3 lastDirection;
        Vec3 lastVelocity = Vec3.ZERO;
        double lastSampleTime = -1.0d;
        double lastRenderTime = -1.0d;
        float coverage;
        float targetCoverage;
        boolean active;
        int quietSamples;
        float power;
        ThrusterPlumeSpec.Style style = ThrusterPlumeSpec.Style.FIRE;

        void add(TrailNode node) {
            nodes.add(node);
            while (nodes.size() > PlumeTrailMath.MAX_TRAIL_NODES) nodes.remove(0);
        }
    }
}
