package dev.propulsionteam.propulsionsimulated.content.toolgun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.simibubi.create.content.equipment.zapper.ZapperRenderHandler;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionItems;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix4f;

import java.util.LinkedList;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ToolgunRenderHandler extends ZapperRenderHandler {
    private static ToolgunRenderHandler instance = new ToolgunRenderHandler();
    static final Vec3 HANDLE_GRIP = new Vec3((4.90119 - Math.sqrt(0.125)) / 16,
        (1.27373 - Math.sqrt(0.125)) / 16, 0.5);
    static final Vec3 GRIP_OFFSET = new Vec3(-0.65, 0, 0.65);
    static final Vec3 GRIP_ROTATION = new Vec3(90, 0, 3.5);
    static final Vec3 ITEM_OFFSET = new Vec3(0, 1.3, -1.3);

    @Override
    protected boolean appliesTo(ItemStack stack) {
        return stack.is(PropulsionItems.TOOLGUN.get());
    }

    @Override
    protected void onRenderPlayerHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (!appliesTo(stack)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        boolean rightHand = event.getHand() == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
        float flip = rightHand ? 1 : -1;
        float partialTick = event.getPartialTick();
        float recoil = getAnimation(rightHand, partialTick);
        float equipProgress = event.getEquipProgress();
        if (rightHand ? rightHandAnimation > 0.01f || dontReequipRight : leftHandAnimation > 0.01f || dontReequipLeft) {
            equipProgress = 0;
        }

        PoseStack poseStack = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        ItemDisplayContext context = rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        poseStack.pushPose();
        applyToolPose(poseStack, flip, event.getSwingProgress(), equipProgress, recoil);
        transformTool(poseStack, flip, equipProgress, recoil, partialTick);
        mc.getEntityRenderDispatcher().getItemInHandRenderer()
            .renderItem(player, stack, context, !rightHand, poseStack, buffers, light);

        poseStack.pushPose();
        var model = mc.getItemRenderer().getModel(stack, mc.level, player, player.getId() + context.ordinal());
        ClientHooks.handleCameraTransforms(poseStack, model, context, !rightHand);
        poseStack.translate(-0.5, -0.5, -0.5);
        Matrix4f hand = handPose(poseStack.last().pose(), rightHand, player.getSkin().model() == PlayerSkin.Model.SLIM);
        poseStack.setIdentity();
        poseStack.mulPose(hand);
        RenderSystem.setShaderTexture(0, player.getSkin().texture());
        var renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        if (rightHand) {
            renderer.renderRightHand(poseStack, buffers, light, player);
        } else {
            renderer.renderLeftHand(poseStack, buffers, light, player);
        }
        poseStack.popPose();
        poseStack.popPose();
        event.setCanceled(true);
    }

    static void applyToolPose(PoseStack poseStack, float flip, float swingProgress, float equipProgress, float recoil) {
        float swing = Mth.sqrt(swingProgress);
        float x = -0.3f * Mth.sin(swing * Mth.PI);
        float y = 0.4f * Mth.sin(swing * Mth.TWO_PI);
        float z = -0.4f * Mth.sin(swingProgress * Mth.PI);
        float roll = Mth.sin(swingProgress * swingProgress * Mth.PI);
        float yaw = Mth.sin(swing * Mth.PI);
        poseStack.translate(flip * (x + 0.64f - 0.1f), y - 0.4f - equipProgress * 0.6f, z - 0.72f - 0.1f + recoil);
        poseStack.mulPose(Axis.YP.rotationDegrees(flip * yaw * 70));
        poseStack.mulPose(Axis.ZP.rotationDegrees(flip * roll * -20));
    }

    static Matrix4f handPose(Matrix4f toolModel, boolean rightHand, boolean slim) {
        float palmX = (slim ? 5.5f : 6) / 16;
        return new Matrix4f(toolModel)
            .translate((float) (HANDLE_GRIP.x + GRIP_OFFSET.x / 16),
                (float) (HANDLE_GRIP.y + GRIP_OFFSET.y / 16), (float) (HANDLE_GRIP.z + GRIP_OFFSET.z / 16))
            .rotateXYZ((float) Math.toRadians(GRIP_ROTATION.x), (float) Math.toRadians(GRIP_ROTATION.y),
                (float) Math.toRadians(GRIP_ROTATION.z))
            .rotateZ(-Mth.HALF_PI).rotateY(rightHand ? 0 : Mth.PI)
            .translate(rightHand ? palmX : -palmX, -10 / 16f, 0);
    }

    @Override
    protected void transformTool(PoseStack poseStack, float flip, float equipProgress, float recoil, float partialTick) {
        super.transformTool(poseStack, flip, equipProgress, recoil, partialTick);
        poseStack.translate(ITEM_OFFSET.x / 16, ITEM_OFFSET.y / 16, ITEM_OFFSET.z / 16);
    }

    public static void fire(InteractionHand hand, Vec3 start, Vec3 end, boolean hitBlock) {
        if (instance.cachedBeams == null) {
            instance.cachedBeams = new LinkedList<>();
        }
        LaserBeam beam = new LaserBeam(start, end);
        if (hitBlock) {
            instance.addBeam(beam);
        } else {
            instance.cachedBeams.add(beam);
        }
        instance.shoot(hand, start);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        instance.onRenderPlayerHand(event);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) {
            instance.tick();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (instance.cachedBeams != null) {
            instance.cachedBeams.forEach(beam -> Outliner.getInstance().remove(beam));
        }
        instance = new ToolgunRenderHandler();
    }
}
