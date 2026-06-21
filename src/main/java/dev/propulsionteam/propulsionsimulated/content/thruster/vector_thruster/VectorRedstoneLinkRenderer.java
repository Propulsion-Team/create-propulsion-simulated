package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import com.mojang.datafixers.util.Pair;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster.LiquidVectorThrusterBlockEntity;
import dev.ryanhcode.sable.Sable;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VectorRedstoneLinkRenderer {

    private static final BehaviourType<VectorRedstoneLinkBehaviour>[] ALL_TYPES = new BehaviourType[]{
        VectorRedstoneLinkBehaviour.WEST_TYPE,
        VectorRedstoneLinkBehaviour.EAST_TYPE,
        VectorRedstoneLinkBehaviour.DOWN_TYPE,
        VectorRedstoneLinkBehaviour.UP_TYPE
    };

    private static final String[] SIDE_KEYS = { "west", "east", "down", "up" };

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        HitResult target = mc.hitResult;
        if (!(target instanceof BlockHitResult result))
            return;

        ClientLevel world = mc.level;
        if (world == null)
            return;
        BlockPos pos = result.getBlockPos();

        Component freq1 = CreateLang.translateDirect("logistics.firstFrequency");
        Component freq2 = CreateLang.translateDirect("logistics.secondFrequency");

        for (int i = 0; i < ALL_TYPES.length; i++) {
            VectorRedstoneLinkBehaviour behaviour = BlockEntityBehaviour.get(world, pos, ALL_TYPES[i]);
            if (behaviour == null)
                continue;

            String sideKey = SIDE_KEYS[i];
            for (boolean first : Iterate.trueAndFalse) {
                AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.25f);
                Component label = first ? freq1 : freq2;
                boolean hit = behaviour.testHit(first, target.getLocation());
                ValueBoxTransform transform = first ? behaviour.getFirstSlot() : behaviour.getSecondSlot();

                ValueBox box = new ValueBox(label, bb, pos).passive(!hit);
                boolean empty = behaviour.getFrequency(first).getStack().isEmpty();
                if (!empty)
                    box.wideOutline();

                Outliner.getInstance()
                    .showOutline(Pair.of("vector_link_" + sideKey + "_" + first, pos), box.transform(transform))
                    .highlightFace(result.getDirection());

                if (!hit)
                    continue;

                List<MutableComponent> tip = new ArrayList<>();
                tip.add(label.copy());
                tip.add(CreateLang.translateDirect(
                    empty ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace"));
                CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
            }
        }
    }

    public static void renderOnBlockEntity(VectorThrusterBlockEntity be, float partialTicks, PoseStack ms,
                                           MultiBufferSource buffer, int light, int overlay) {
        if (be == null || be.isRemoved())
            return;

        if (!be.isVirtual()) {
            if (Minecraft.getInstance().cameraEntity == null)
                return;
            float max = com.simibubi.create.infrastructure.config.AllConfigs.client().filterItemRenderDistance.getF();
            if (Minecraft.getInstance().cameraEntity.position()
                     .distanceToSqr(Sable.HELPER.projectOutOfSubLevel(be.getLevel(), VecHelper.getCenterOf(be.getBlockPos()))) > (max * max))
                return;
        }

        VectorRedstoneLinkBehaviour[] links = {
            be.westLink, be.eastLink, be.downLink, be.upLink
        };

        for (VectorRedstoneLinkBehaviour behaviour : links) {
            if (behaviour == null) continue;
            for (boolean first : Iterate.trueAndFalse) {
                ValueBoxTransform transform = first ? behaviour.getFirstSlot() : behaviour.getSecondSlot();
                ms.pushPose();
                transform.transform(be.getLevel(), be.getBlockPos(), be.getBlockState(), ms);
                ValueBoxRenderer.renderItemIntoValueBox(behaviour.getFrequency(first).getStack(), ms, buffer, light, overlay);
                ms.popPose();
            }
        }
    }

    public static void renderOnBlockEntity(LiquidVectorThrusterBlockEntity be, float partialTicks, PoseStack ms,
                                           MultiBufferSource buffer, int light, int overlay) {
        if (be == null || be.isRemoved())
            return;

        if (!be.isVirtual()) {
            if (Minecraft.getInstance().cameraEntity == null)
                return;
            float max = com.simibubi.create.infrastructure.config.AllConfigs.client().filterItemRenderDistance.getF();
            if (Minecraft.getInstance().cameraEntity.position()
                     .distanceToSqr(Sable.HELPER.projectOutOfSubLevel(be.getLevel(), VecHelper.getCenterOf(be.getBlockPos()))) > (max * max))
                return;
        }

        VectorRedstoneLinkBehaviour[] links = {
            be.westLink, be.eastLink, be.downLink, be.upLink
        };

        for (VectorRedstoneLinkBehaviour behaviour : links) {
            if (behaviour == null) continue;
            for (boolean first : Iterate.trueAndFalse) {
                ValueBoxTransform transform = first ? behaviour.getFirstSlot() : behaviour.getSecondSlot();
                ms.pushPose();
                transform.transform(be.getLevel(), be.getBlockPos(), be.getBlockState(), ms);
                ValueBoxRenderer.renderItemIntoValueBox(behaviour.getFrequency(first).getStack(), ms, buffer, light, overlay);
                ms.popPose();
            }
        }
    }
}
