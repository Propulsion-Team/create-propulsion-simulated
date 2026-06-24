package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.MeshedThrusterFlameUtils;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;

import java.util.List;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.joml.Vector3d;

public class VectorThrusterBlockEntity extends IonThrusterBlockEntity {
    public static final float MAX_VISUAL_TILT_DEGREES = 30.0f;
    private static final float TWEEN_SPEED = 0.2f;

    public VectorRedstoneLinkBehaviour westLink;
    public VectorRedstoneLinkBehaviour eastLink;
    public VectorRedstoneLinkBehaviour downLink;
    public VectorRedstoneLinkBehaviour upLink;

    private int westSignal;
    private int eastSignal;
    private int downSignal;
    private int upSignal;

    private float targetVectorX;
    private float targetVectorY;
    private float currentVectorX;
    private float currentVectorY;
    private float prevVectorX;
    private float prevVectorY;
    private float obstructionEfficiency = 1.0f;
    private boolean clientInitialized = false;

    // Flap animation: 0 = idle (wide), 1 = full throttle (narrow)
    private float targetFlapProgress;
    private float currentFlapProgress;
    private float prevFlapProgress;

    public VectorThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected boolean supportsMultiblock() {
        return false;
    }

    protected VectorThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PropulsionConfig.ThrusterPlumeType getPlumeRenderType() {
        return PropulsionConfig.getVectorThrustersPlumeType();
    }


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        westLink = VectorRedstoneLinkBehaviour.receiver(this,
                ValueBoxTransform.Dual.makeSlots(isFirst -> new VectorThrusterLinkTransform(isFirst, Direction.WEST)),
                VectorRedstoneLinkBehaviour.WEST_TYPE, "West",
                power -> setSignal(power, Direction.WEST));

        eastLink = VectorRedstoneLinkBehaviour.receiver(this,
                ValueBoxTransform.Dual.makeSlots(isFirst -> new VectorThrusterLinkTransform(isFirst, Direction.EAST)),
                VectorRedstoneLinkBehaviour.EAST_TYPE, "East",
                power -> setSignal(power, Direction.EAST));

        downLink = VectorRedstoneLinkBehaviour.receiver(this,
                ValueBoxTransform.Dual.makeSlots(isFirst -> new VectorThrusterLinkTransform(isFirst, Direction.DOWN)),
                VectorRedstoneLinkBehaviour.DOWN_TYPE, "Down",
                power -> setSignal(power, Direction.DOWN));

        upLink = VectorRedstoneLinkBehaviour.receiver(this,
                ValueBoxTransform.Dual.makeSlots(isFirst -> new VectorThrusterLinkTransform(isFirst, Direction.UP)),
                VectorRedstoneLinkBehaviour.UP_TYPE, "Up",
                power -> setSignal(power, Direction.UP));

        behaviours.add(westLink);
        behaviours.add(eastLink);
        behaviours.add(downLink);
        behaviours.add(upLink);
    }

    private void setSignal(int power, Direction localSide) {
        int clamped = Math.clamp(power, 0, 15);
        int prev = switch (localSide) {
            case WEST -> westSignal;
            case EAST -> eastSignal;
            case DOWN -> downSignal;
            case UP -> upSignal;
            default -> 0;
        };
        if (prev == clamped) return;
        switch (localSide) {
            case WEST -> westSignal = clamped;
            case EAST -> eastSignal = clamped;
            case DOWN -> downSignal = clamped;
            case UP -> upSignal = clamped;
            default -> {
            }
        }
        onVectorSignalChanged();
    }

    // --- CC / external API -------------------------------------------------

    public float getTargetVectorX() {
        return targetVectorX;
    }

    public float getTargetVectorY() {
        return targetVectorY;
    }

    public float getCurrentVectorX() {
        return currentVectorX;
    }

    public float getCurrentVectorY() {
        return currentVectorY;
    }

    public float getInterpolatedVectorX(float partialTick) {
        return Mth.lerp(partialTick, prevVectorX, currentVectorX);
    }

    public float getInterpolatedVectorY(float partialTick) {
        return Mth.lerp(partialTick, prevVectorY, currentVectorY);
    }

    public float getInterpolatedFlapProgress(float partialTick) {
        return Mth.lerp(partialTick, prevFlapProgress, currentFlapProgress);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return MeshedThrusterFlameUtils.inflateVectorRenderBoundingBox(this, super.getRenderBoundingBox());
    }

    /**
     * Sets the four directional signals to produce the given -1..1 vector.
     */
    public void setVectorCoordinates(float x, float y) {
        westSignal = x > 0 ? Math.round(x * 15) : 0;
        eastSignal = x < 0 ? Math.round(-x * 15) : 0;
        downSignal = y > 0 ? Math.round(y * 15) : 0;
        upSignal = y < 0 ? Math.round(-y * 15) : 0;
        onVectorSignalChanged();
    }

    /**
     * Ponder (and similar): set redstone-derived targets while the scene advances animation manually.
     */
    public void applyVectorSignalsForScene(float x, float y) {
        setVectorCoordinates(x, y);
    }

    public void animateVectorForScene() {
        prevVectorX = currentVectorX;
        prevVectorY = currentVectorY;
        currentVectorX = tweenTowards(currentVectorX, targetVectorX);
        currentVectorY = tweenTowards(currentVectorY, targetVectorY);
    }

    // -----------------------------------------------------------------------

    @Override
    public void tick() {
        updateMappedTargets();
        prevVectorX = currentVectorX;
        prevVectorY = currentVectorY;
        currentVectorX = tweenTowards(currentVectorX, targetVectorX);
        currentVectorY = tweenTowards(currentVectorY, targetVectorY);

        targetFlapProgress = (float) getThrottle();
        prevFlapProgress = currentFlapProgress;
        currentFlapProgress = tweenTowards(currentFlapProgress, targetFlapProgress);

        super.tick();
    }

    @Override
    public Vector3d getThrustDirectionLocal() {
        Vector3d localExhaust = computeExhaustDirectionLocal();
        if (localExhaust.lengthSquared() < 1e-8) {
            Vector3d forward = new Vector3d(getFacing().getStepX(), getFacing().getStepY(), getFacing().getStepZ()).normalize();
            return forward;
        }
        return localExhaust.negate();
    }

    @Override
    protected Vec3 getParticleExhaustDirectionLocal() {
        Vector3d exhaust = computeExhaustDirectionLocal();
        return new Vec3(exhaust.x, exhaust.y, exhaust.z);
    }

    private Vector3d computeExhaustDirectionLocal() {
        Vector3d forward = new Vector3d(getFacing().getStepX(), getFacing().getStepY(), getFacing().getStepZ()).normalize();
        Vector3d right = computeRight(forward);
        Vector3d up = new Vector3d(right).cross(forward).normalize();
        double tiltScale = Math.tan(Math.toRadians(MAX_VISUAL_TILT_DEGREES));

        Vector3d exhaust = new Vector3d(forward).negate()
                .fma(currentVectorX * tiltScale, right)
                .fma(currentVectorY * tiltScale, up);

        if (exhaust.lengthSquared() < 1e-8) {
            exhaust.set(forward).negate();
        } else {
            exhaust.normalize();
        }
        return exhaust;
    }

    @Override
    protected Vec3 getLocalNozzlePosition(BlockPos pos, Vec3 localExhaustDirection, double nozzleOffset) {
        Vector3d forward = new Vector3d(getFacing().getStepX(), getFacing().getStepY(), getFacing().getStepZ()).normalize();
        Vector3d right = computeRight(forward);
        Vector3d up = new Vector3d(right).cross(forward).normalize();

        Vector3d center = new Vector3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d);
        Vector3d pivot = new Vector3d(center)
                .fma(-1.0d / 16.0d, right)
                .fma(1.0d / 16.0d, up)
                .fma(7.0d / 16.0d, forward);

        Vector3d exhaust = new Vector3d(localExhaustDirection.x, localExhaustDirection.y, localExhaustDirection.z);
        double tiltScale = Math.tan(Math.toRadians(MAX_VISUAL_TILT_DEGREES));
        double yawRad = Math.toRadians(Mth.clamp((float) (exhaust.dot(right) / tiltScale), -1f, 1f) * MAX_VISUAL_TILT_DEGREES);
        double pitchRad = Math.toRadians(-Mth.clamp((float) (exhaust.dot(up) / tiltScale), -1f, 1f) * MAX_VISUAL_TILT_DEGREES);

        Vector3d neutralNozzle = new Vector3d(center).fma(-nozzleOffset, forward);
        Vector3d relative = neutralNozzle.sub(pivot, new Vector3d());

        rotateAroundAxis(relative, up, yawRad);
        rotateAroundAxis(relative, right, pitchRad);

        Vector3d rotatedNozzle = pivot.add(relative, new Vector3d());
        return new Vec3(rotatedNozzle.x, rotatedNozzle.y, rotatedNozzle.z);
    }

    @Override
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
        int scanLength = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
        ObstructionRaySample sample = sampleObstructionRaycast(level, scanLength);
        double firstHitDistance = sample.firstHitDistance();
        float newEfficiency = scanLength <= 0
                ? 0.0f
                : Math.clamp((float) (firstHitDistance / scanLength), 0.0f, 1.0f);
        int newEmptyBlocks = sample.emptyBlocksEstimate();

        if (this.emptyBlocks != newEmptyBlocks || Math.abs(this.obstructionEfficiency - newEfficiency) > 1e-4f) {
            this.emptyBlocks = newEmptyBlocks;
            this.obstructionEfficiency = newEfficiency;
            this.isThrustDirty = true;
        }
    }

    @Override
    protected float calculateObstructionEffect() {
        return obstructionEfficiency;
    }

    private void onVectorSignalChanged() {
        updateMappedTargets();
        if (level != null && !level.isClientSide) {
            setChanged();
            notifyUpdate();
        }
    }

    private void updateMappedTargets() {
        // West signal tilts nozzle right (+X); East tilts it left (-X).
        // Down signal tilts nozzle up (+Y); Up tilts it down (-Y).
        targetVectorX = Mth.clamp((westSignal - eastSignal) / 15.0f, -1.0f, 1.0f);
        targetVectorY = Mth.clamp((downSignal - upSignal) / 15.0f, -1.0f, 1.0f);
    }

    private static float tweenTowards(float current, float target) {
        float next = current + (target - current) * TWEEN_SPEED;
        if (Math.abs(target - next) < 0.001f) return target;
        return next;
    }

    private static Vector3d computeRight(Vector3d forward) {
        Vector3d reference = Math.abs(forward.y) > 0.999 ? new Vector3d(0, 0, 1) : new Vector3d(0, 1, 0);
        Vector3d right = new Vector3d(forward).cross(reference);
        if (right.lengthSquared() < 1e-8) return new Vector3d(1, 0, 0);
        right.normalize();
        if (Math.abs(forward.y) > 0.999) right.negate();
        return right;
    }

    private static void rotateAroundAxis(Vector3d v, Vector3d axisUnit, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double dot = v.dot(axisUnit);
        Vector3d cross = new Vector3d(axisUnit).cross(v);
        Vector3d rotated = new Vector3d(v).mul(cos)
                .add(cross.mul(sin))
                .add(new Vector3d(axisUnit).mul(dot * (1.0d - cos)));
        v.set(rotated);
    }

    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("WestSignal", westSignal);
        compound.putInt("EastSignal", eastSignal);
        compound.putInt("DownSignal", downSignal);
        compound.putInt("UpSignal", upSignal);
        compound.putFloat("TargetVectorX", targetVectorX);
        compound.putFloat("TargetVectorY", targetVectorY);
        compound.putFloat("CurrentVectorX", currentVectorX);
        compound.putFloat("CurrentVectorY", currentVectorY);
        compound.putFloat("ObstructionEfficiency", obstructionEfficiency);
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        westSignal = compound.getInt("WestSignal");
        eastSignal = compound.getInt("EastSignal");
        downSignal = compound.getInt("DownSignal");
        upSignal = compound.getInt("UpSignal");
        updateMappedTargets();
        targetVectorX = compound.contains("TargetVectorX") ? compound.getFloat("TargetVectorX") : targetVectorX;
        targetVectorY = compound.contains("TargetVectorY") ? compound.getFloat("TargetVectorY") : targetVectorY;
        if (!clientPacket || !clientInitialized) {
            currentVectorX = compound.contains("CurrentVectorX") ? compound.getFloat("CurrentVectorX") : targetVectorX;
            currentVectorY = compound.contains("CurrentVectorY") ? compound.getFloat("CurrentVectorY") : targetVectorY;
            prevVectorX = currentVectorX;
            prevVectorY = currentVectorY;
            if (clientPacket) clientInitialized = true;
        }
        int scanLength = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
        obstructionEfficiency = compound.contains("ObstructionEfficiency")
                ? compound.getFloat("ObstructionEfficiency")
                : (scanLength <= 0 ? 0.0f : Math.clamp((float) emptyBlocks / (float) scanLength, 0.0f, 1.0f));
    }

    @Override
    public double getNozzleOffsetFromCenter() {
        return PropulsionConfig.NOZZLE_OFFSET_FROM_CENTER.get();
    }

    @Override
    protected double getBaseThrust() {
        return PropulsionConfig.VECTOR_THRUSTER_BASE_THRUST.get();
    }

    @Override
    protected double getRawThrustCap() {
        return PropulsionConfig.VECTOR_THRUSTER_BASE_THRUST.get();
    }

    @Override
    protected ParticleOptions createParticleOptions() {
        // Particle narrows as the nozzle closes: 0.85 at idle, 0.35 at full throttle
        float size = Mth.lerp(currentFlapProgress, 0.85f, 0.35f);
        return new IonParticleData(List.of(), getDyeColor(), size);
    }

    // -----------------------------------------------------------------------

    private static class VectorThrusterLinkTransform extends ValueBoxTransform.Dual {
        /**
         * Local side of the input block face this slot lives on (WEST/EAST/DOWN/UP).
         */
        private final Direction localSide;

        public VectorThrusterLinkTransform(boolean first, Direction localSide) {
            super(first);
            this.localSide = localSide;
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Vec3 local = switch (localSide) {
                // Outer face of each 1px-thick cube is at x=1 / x=15 / y=1 / y=15.
                // Use 0.5 to sit just outside that face so the box is visible.
                case WEST -> VecHelper.voxelSpace(0.5f, isFirst() ? 11f : 5f, 2f);
                case EAST -> VecHelper.voxelSpace(15.5f, isFirst() ? 11f : 5f, 2f);
                case DOWN -> VecHelper.voxelSpace(isFirst() ? 5f : 11f, 0.5f, 2f);
                case UP -> VecHelper.voxelSpace(isFirst() ? 5f : 11f, 15.5f, 2f);
                default -> Vec3.ZERO;
            };
            return rotatePointForFacing(local, state.getValue(AbstractThrusterBlock.FACING));
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction worldSide = getWorldSide(state.getValue(AbstractThrusterBlock.FACING));
            float yRot = AngleHelper.horizontalAngle(worldSide) + 180;
            float xRot = worldSide == Direction.UP ? 90 : worldSide == Direction.DOWN ? 270 : 0;
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
            ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(xRot));
        }

        @Override
        public void transform(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            super.transform(level, pos, state, ms);
            ms.scale(0.75f, 0.75f, 0.75f);
        }

        @Override
        public float getScale() {
            return 0.4975f;
        }

        private Direction getWorldSide(Direction blockFacing) {
            Vec3 sideNormal = Vec3.atLowerCornerOf(localSide.getNormal());
            Vec3 rotated = rotateDirectionForFacing(sideNormal, blockFacing);
            return Direction.getNearest(rotated.x, rotated.y, rotated.z);
        }

        private Vec3 rotatePointForFacing(Vec3 vec, Direction blockFacing) {
            return switch (blockFacing) {
                case NORTH -> vec;
                case EAST -> VecHelper.rotateCentered(vec, -90, Direction.Axis.Y);
                case SOUTH -> VecHelper.rotateCentered(vec, 180, Direction.Axis.Y);
                case WEST -> VecHelper.rotateCentered(vec, 90, Direction.Axis.Y);
                case UP -> VecHelper.rotateCentered(vec, 90, Direction.Axis.X);
                case DOWN -> VecHelper.rotateCentered(vec, -90, Direction.Axis.X);
            };
        }

        private Vec3 rotateDirectionForFacing(Vec3 vec, Direction blockFacing) {
            return switch (blockFacing) {
                case NORTH -> vec;
                case EAST -> VecHelper.rotate(vec, -90, Direction.Axis.Y);
                case SOUTH -> VecHelper.rotate(vec, 180, Direction.Axis.Y);
                case WEST -> VecHelper.rotate(vec, 90, Direction.Axis.Y);
                case UP -> VecHelper.rotate(vec, 90, Direction.Axis.X);
                case DOWN -> VecHelper.rotate(vec, -90, Direction.Axis.X);
            };
        }
    }
}
