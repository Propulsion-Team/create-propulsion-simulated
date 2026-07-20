package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.minecraft.util.Mth;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TiltAdapterRenderer extends KineticBlockEntityRenderer<TiltAdapterBlockEntity> {
    public TiltAdapterRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

    private static final Vector3f AOM_INPUT = new Vector3f();
    private static final Vector3f AOM_OUTPUT = new Vector3f();

    private static final Quaternionf ROT_Y_270 = Axis.YP.rotationDegrees(270.0f);
    private static final Quaternionf ROT_Y_180 = Axis.YP.rotationDegrees(180.0f);
    private static final Quaternionf ROT_Z_180 = Axis.ZP.rotationDegrees(180.0f);


    @Override
	protected void renderSafe(TiltAdapterBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        Block block = blockEntity.getBlockState().getBlock();
		final net.minecraft.core.Direction.Axis axis = ((IRotate) block).getRotationAxis(blockEntity.getBlockState());
		final BlockPos pos = blockEntity.getBlockPos();
        final BlockState blockState = blockEntity.getBlockState();
        final Direction direction = AbstractTiltAdapterBlock.getDirection(blockState);
        final Direction invDirection = direction.getOpposite();
        final boolean alignedX = AbstractTiltAdapterBlock.isAxisAlongFirst(blockState);
        final boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
		float time = AnimationTickHolder.getRenderTime(level);

        SuperByteBuffer inputShaft = CachedBuffers.partialFacing(PropulsionPartialModels.TILT_ADAPTER_INPUT_SHAFT, blockState, invDirection);
        SuperByteBuffer outputShaft = CachedBuffers.partialFacing(PropulsionPartialModels.TILT_ADAPTER_OUTPUT_SHAFT, blockState, invDirection);

        float angle = (time * blockEntity.getSpeed() * 3f / 10) % 360;
        float offset = getRotationOffsetForPosition(blockEntity, pos, axis);
        float outputModifier = blockEntity.getRotationSpeedModifier(direction);

        AOM_INPUT.set(angle, offset, 1);
        AOM_OUTPUT.set(angle, offset, outputModifier);

        renderShaft(AOM_INPUT, inputShaft, blockEntity, axis, light, ms, buffer);
        renderShaft(AOM_OUTPUT, outputShaft, blockEntity, axis, light, ms, buffer);
        if (!(blockEntity instanceof AdvancedTiltAdapterBlockEntity)) {
            renderOverlays(blockEntity, blockState, invDirection, getGantryTiltRadians(blockEntity, partialTicks), light, ms, buffer, direction, alignedX, positive);
        }
    }

    /** Smooth client display between synced {@link TiltAdapterBlockEntity#getCurrentAngle()} updates. */
    private static float getGantryTiltRadians(TiltAdapterBlockEntity blockEntity, float partialTicks) {
        float degrees = blockEntity.getCurrentAngle();
        float target = blockEntity.getRenderTargetAngle();
        float speed = Math.abs(blockEntity.getSpeed());
        if (speed > 0 && Math.abs(target - degrees) > 0.001f) {
            float maxStep = KineticBlockEntity.convertToAngular(speed) * partialTicks;
            degrees += Mth.clamp(target - degrees, -maxStep, maxStep);
        }
        return degrees / 180f * (float) Math.PI;
    }

    private void renderShaft(Vector3f aom, SuperByteBuffer shaft, TiltAdapterBlockEntity blockEntity, net.minecraft.core.Direction.Axis axis, int light, PoseStack ms, MultiBufferSource buffer) {
        float angle = getAngle(aom);
        kineticRotationTransform(shaft, blockEntity, axis, angle, light);
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    private void renderOverlays(TiltAdapterBlockEntity blockEntity, BlockState blockState, Direction invDirection, float gantryTilt, int light, PoseStack ms, MultiBufferSource buffer, Direction direction, boolean alignedX, boolean positive) {
        float redstoneLeft = blockEntity.getLeft() / 15.0f;
        float redstoneRight = blockEntity.getRight() / 15.0f;

        SuperByteBuffer gantry = CachedBuffers.partialFacing(PropulsionPartialModels.TILT_ADAPTER_GANTRY, blockState, invDirection);
        SuperByteBuffer sideOverlay = CachedBuffers.partialFacing(PropulsionPartialModels.TILT_ADAPTER_SIDE_INDICATOR, blockState, invDirection);

        renderOverlaySide(redstoneLeft, gantryTilt, gantry, sideOverlay, light, ms, buffer, direction, true, alignedX, positive);
        renderOverlaySide(redstoneRight, gantryTilt, gantry, sideOverlay, light, ms, buffer, direction, false, alignedX, positive);
    }

    private void renderOverlaySide(float redstoneSignal, float gantryTilt, SuperByteBuffer gantry, SuperByteBuffer sideOverlay, int light, PoseStack ms, MultiBufferSource buffer, Direction direction, boolean isRight, boolean alignedX, boolean positive) {
        int color = Color.mixColors(0x470102, 0xCD0000, redstoneSignal);
        boolean isHorizontal = direction.getAxis().isHorizontal();
        Axis rotationAxis = isHorizontal ? Axis.YP : Axis.ZP;
        boolean flipRightCondition = !isHorizontal && (!positive ^ alignedX);
        boolean shouldFlip = isRight ^ flipRightCondition;
        float offset = shouldFlip ? 1/16.0f : -1/16.0f;

        ms.pushPose();
        
        applyLocalTransforms(ms, alignedX, shouldFlip, rotationAxis);
        translateInDirection(ms, direction, offset);
        sideOverlay.color(color).light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));

        Direction offsetDirection = isHorizontal ? direction.getClockWise() : Direction.EAST;
        Direction rotationDirection = isHorizontal ? (positive ? Direction.DOWN : Direction.UP) : Direction.NORTH;

        offset = -(0.5f + 4) / 16.0f;
        translateInDirection(ms, offsetDirection, offset);
        gantry.rotateCentered(gantryTilt, rotationDirection).color(color).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        ms.popPose();
    }

    private void applyLocalTransforms(PoseStack ms, boolean alignedX, boolean shouldFlip, Axis rotationAxis) {
        ms.translate(0.5, 0.5, 0.5);
        if (alignedX) {
            ms.mulPose(ROT_Y_270);
        }
        
        if (shouldFlip) {
            ms.mulPose(rotationAxis == com.mojang.math.Axis.YP ? ROT_Y_180 : ROT_Z_180);
        }
        ms.translate(-0.5, -0.5, -0.5);
    }

    private void translateInDirection(PoseStack ms, Direction direction, float offset) {
        ms.translate(direction.getStepX() * offset, direction.getStepY() * offset, direction.getStepZ() * offset);
    }

    private float getAngle(Vector3f aom) {
        float angle = aom.x;
        angle *= aom.z;
        angle += aom.y;
        angle = angle / 180f * (float) Math.PI;
        return angle;
    }
}
