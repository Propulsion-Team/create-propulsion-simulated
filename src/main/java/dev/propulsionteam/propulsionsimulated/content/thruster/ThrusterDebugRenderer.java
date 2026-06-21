package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.debug.DebugRenderer;
import dev.propulsionteam.propulsionsimulated.debug.PropulsionDebug;
import dev.propulsionteam.propulsionsimulated.debug.routes.MainDebugRoute;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public final class ThrusterDebugRenderer {
    private static final int RENDER_TICKS = 2;
    private static final float LINE_THICKNESS = 0.07f;
    private static final double STEP = 0.1d;
    private static final double START_EPSILON = 0.05d;

    private ThrusterDebugRenderer() {
    }

    public static void debug_drawRenderBoundingBox(AbstractThrusterBlockEntity be, PoseStack ms, MultiBufferSource buffer) {
        //Debug render box
//        ms.pushPose();
//        ms.setIdentity();
        AABB box = be.getRenderBoundingBox();
        BlockPos pos2 = be.getBlockPos();
        AABB localBox = box.move(-pos2.getX(), -pos2.getY(), -pos2.getZ());
        LevelRenderer.renderLineBox(ms, buffer.getBuffer(RenderType.lines()), localBox, 1, 0, 0, 1);
//        ms.popPose();
    }

    public static void render(AbstractThrusterBlockEntity be, PoseStack ms, MultiBufferSource buffer) {
        if (be == null || be.isRemoved() || be.getLevel() == null) {
            return;
        }
        debug_drawRenderBoundingBox(be, ms, buffer);

        if (!PropulsionDebug.isDebug(MainDebugRoute.THRUSTER)) {
            return;
        }

        AbstractThrusterBlockEntity.WorldExhaustRay worldRay = be.getWorldExhaustRay();
        if (worldRay == null || worldRay.direction().lengthSqr() < 1.0e-8) {
            return;
        }

        int obstructionLength = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
        Level worldLevel = worldRay.level();
        Vec3 traceStart = worldRay.nozzlePos().add(worldRay.direction().scale(START_EPSILON));
        Vec3 worldEnd = traceStart.add(worldRay.direction().scale(obstructionLength));

        String idBase = "thruster_debug_" + be.getBlockPos().asLong();

        DebugRenderer.drawElongatedBox(
            idBase + "_ray",
            worldRay.nozzlePos(),
            worldEnd,
            LINE_THICKNESS,
            new Color(0, 255, 255, 255),
            false,
            RENDER_TICKS
        );
        DebugRenderer.drawBox(idBase + "_origin", worldRay.nozzlePos(), new Vec3(0.12, 0.12, 0.12), Color.GREEN, RENDER_TICKS);

        BlockHitResult hitResult = worldLevel.clip(new ClipContext(
            traceStart,
            worldEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            net.minecraft.world.phys.shapes.CollisionContext.empty()
        ));
        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            DebugRenderer.drawBox(
                idBase + "_ray_hit",
                hitResult.getLocation(),
                new Vec3(0.14, 0.14, 0.14),
                new Color(255, 100, 100, 255),
                RENDER_TICKS
            );
        }

        Set<Long> sampled = new HashSet<>();
        int hitIndex = 0;
        BlockPos selfPos = BlockPos.containing(worldRay.nozzlePos());
        long lastPosKey = Long.MIN_VALUE;
        for (double t = 0.0d; t <= obstructionLength; t += STEP) {
            Vec3 sample = traceStart.add(worldRay.direction().scale(t));
            BlockPos hitPos = BlockPos.containing(sample);
            long key = hitPos.asLong();
            if (key == lastPosKey) {
                continue;
            }
            lastPosKey = key;
            if (hitPos.equals(selfPos) || !sampled.add(key)) {
                continue;
            }

            BlockState stateAt = worldLevel.getBlockState(hitPos);
            if (stateAt.isAir() || !stateAt.isSolid()) {
                continue;
            }

            DebugRenderer.drawBox(
                idBase + "_hit_" + hitIndex,
                Vec3.atCenterOf(hitPos),
                new Vec3(0.98, 0.98, 0.98),
                new Color(255, 64, 64, 255),
                RENDER_TICKS
            );
            hitIndex++;
        }
    }
}
