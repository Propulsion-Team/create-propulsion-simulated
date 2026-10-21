package dev.propulsionteam.propulsionsimulated.content.toolgun;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler.LaserBeam;
import dev.ryanhcode.sable.Sable;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionItems;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.UnaryOperator;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ToolgunLaserHandler {
    private static final double RANGE = 128;
    private static final int COLOR = 0xFFFFFF;
    private static final int OUTLINE_FADE_TICKS = 10;
    private static Matrix4f inverseWorldClip;
    private static ShotRequest request;
    private static final Map<OutlineKey, Impact> impacts = new HashMap<>();

    private record ShotRequest(ClientLevel level, InteractionHand hand, long tick) {}
    private record OutlineKey(BlockPos pos) {}
    private record Impact(ClientLevel level, double time) {}

    @SubscribeEvent
    public static void onInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isUseItem() || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        InteractionHand hand;
        if (mc.player.getMainHandItem().is(PropulsionItems.TOOLGUN.get())) {
            hand = InteractionHand.MAIN_HAND;
        } else if (mc.player.getOffhandItem().is(PropulsionItems.TOOLGUN.get())) {
            hand = InteractionHand.OFF_HAND;
        } else {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.getHand() == hand) {
            request = new ShotRequest(mc.level, hand, mc.level.getGameTime());
        }
    }

    public static void captureMuzzle(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                    boolean leftHand, PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        if (request == null || entity != mc.player || !stack.is(PropulsionItems.TOOLGUN.get())
                || inverseWorldClip == null || request.level() != mc.level) {
            return;
        }
        HumanoidArm arm = leftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        InteractionHand hand = arm == mc.player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (hand != request.hand() || context.firstPerson() != mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        PoseStack modelPose = new PoseStack();
        modelPose.mulPose(poseStack.last().pose());
        var model = mc.getItemRenderer().getModel(stack, mc.level, entity, entity.getId() + context.ordinal());
        ClientHooks.handleCameraTransforms(modelPose, model, context, leftHand);
        modelPose.translate(-0.5, -0.5, -0.5);
        Vector3f muzzle = muzzlePosition(modelPose.last().pose(), RenderSystem.getModelViewMatrix(),
            RenderSystem.getProjectionMatrix(), inverseWorldClip);
        Vec3 start = mc.gameRenderer.getMainCamera().getPosition().add(muzzle.x(), muzzle.y(), muzzle.z());
        request = null;
        shoot(start, hand);
    }

    static Vector3f muzzlePosition(Matrix4f modelPose, Matrix4f modelView, Matrix4f projection, Matrix4f inverseWorldClip) {
        return new Matrix4f(inverseWorldClip).mul(projection).mul(modelView).mul(modelPose)
            .transformProject(new Vector3f(15 / 16f, 2.5f / 16, 7 / 16f));
    }

    private static void shoot(Vec3 start, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level.clip(new ClipContext(mc.player.getEyePosition(), start, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY, mc.player)).getType() == HitResult.Type.BLOCK) {
            return;
        }

        var camera = mc.gameRenderer.getMainCamera();
        Vector3f look = camera.getLookVector();
        Vec3 origin = camera.getPosition();
        Vec3 target = clip(origin, origin.add(look.x() * RANGE, look.y() * RANGE, look.z() * RANGE)).getLocation();
        Vec3 direction = shotDirection(start, target, position -> Sable.HELPER.projectOutOfSubLevel(mc.level, position));
        if (direction.lengthSqr() > 1e-8) {
            BlockHitResult hit = clip(start, start.add(direction.scale(RANGE)));
            Vec3 end = Sable.HELPER.projectOutOfSubLevel(mc.level, hit.getLocation());
            var effects = CreateClient.ZAPPER_RENDER_HANDLER;
            if (effects.cachedBeams == null) {
                effects.cachedBeams = new LinkedList<>();
            }
            LaserBeam beam = new LaserBeam(start, end);
            if (hit.getType() == HitResult.Type.BLOCK) {
                effects.addBeam(beam);
                double time = mc.level.getGameTime() + camera.getPartialTickTime();
                impacts.put(new OutlineKey(hit.getBlockPos().immutable()), new Impact(mc.level, time));
            } else {
                effects.cachedBeams.add(beam);
            }
            effects.shoot(hand, start);
        }
    }

    private static BlockHitResult clip(Vec3 start, Vec3 end) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE,
            ClipContext.Fluid.ANY, mc.player));
    }

    static Vec3 shotDirection(Vec3 start, Vec3 target, UnaryOperator<Vec3> projectToWorld) {
        return projectToWorld.apply(target).subtract(start).normalize();
    }

    static float outlineAlpha(double age) {
        return (float) Math.clamp(1 - age / OUTLINE_FADE_TICKS, 0, 1);
    }

    private static void updateOutlines(double time) {
        Minecraft mc = Minecraft.getInstance();
        var iterator = impacts.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            float alpha = outlineAlpha(time - entry.getValue().time());
            if (entry.getValue().level() != mc.level || alpha <= 0) {
                Outliner.getInstance().remove(entry.getKey());
                iterator.remove();
                continue;
            }

            Outliner.getInstance().showAABB(entry.getKey(), new AABB(entry.getKey().pos()))
                .colored(new Color(COLOR).setAlpha(alpha))
                .disableLineNormals()
                .lineWidth(alpha / 32f);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        impacts.keySet().forEach(key -> Outliner.getInstance().remove(key));
        impacts.clear();
        request = null;
        inverseWorldClip = null;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            inverseWorldClip = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
            if (request != null && (request.level() != mc.level || mc.level.getGameTime() - request.tick() > 1)) {
                request = null;
            }
            double time = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);
            updateOutlines(time);
        }
    }
}
