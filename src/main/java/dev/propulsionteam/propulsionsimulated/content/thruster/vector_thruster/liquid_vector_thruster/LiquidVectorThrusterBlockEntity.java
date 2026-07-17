package dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterFuelManager;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorRedstoneLinkBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3d;
import java.util.List;

public class LiquidVectorThrusterBlockEntity extends VectorThrusterBlockEntity {
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

    private float targetFlapProgress;
    private float currentFlapProgress;
    private float prevFlapProgress;

    public LiquidVectorThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.LIQUID_VECTOR_THRUSTER_BLOCK_ENTITY.get(), pos, state);
    }

    protected LiquidVectorThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        // VectorThrusterBlockEntity inherits IonThrusterBlockEntity, whose behaviour setup
        // deliberately omits a fluid tank. This variant is fuel-powered, so restore the
        // standard thruster tank and its configured-fuel validation.
        tank = SmartFluidTankBehaviour.single(this, getBaseTankCapacityMb());
        behaviours.add(tank);
        tank.getPrimaryHandler().setValidator(stack -> ThrusterFuelManager.getProperties(stack.getFluid()) != null);

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

    public float getInterpolatedVectorX(float partialTick) {
        return Mth.lerp(partialTick, prevVectorX, currentVectorX);
    }

    public float getInterpolatedVectorY(float partialTick) {
        return Mth.lerp(partialTick, prevVectorY, currentVectorY);
    }

    public float getInterpolatedFlapProgress(float partialTick) {
        return Mth.lerp(partialTick, prevFlapProgress, currentFlapProgress);
    }

    public float getTargetVectorX() { return targetVectorX; }
    public float getTargetVectorY() { return targetVectorY; }
    public float getCurrentVectorX() { return currentVectorX; }
    public float getCurrentVectorY() { return currentVectorY; }

    /** Sets the four directional signals to produce the given -1..1 vector. */
    public void setVectorCoordinates(float x, float y) {
        westSignal = x > 0 ? Math.round(x * 15) : 0;
        eastSignal = x < 0 ? Math.round(-x * 15) : 0;
        downSignal = y > 0 ? Math.round(y * 15) : 0;
        upSignal = y < 0 ? Math.round(-y * 15) : 0;
        onVectorSignalChanged();
    }

    /** Ponder (and similar): set redstone-derived targets while the scene advances animation manually. */
    public void applyVectorSignalsForScene(float x, float y) {
        setVectorCoordinates(x, y);
    }

    public void animateVectorForScene() {
        prevVectorX = currentVectorX;
        prevVectorY = currentVectorY;
        currentVectorX = tweenTowards(currentVectorX, targetVectorX);
        currentVectorY = tweenTowards(currentVectorY, targetVectorY);
    }

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

    @Override
    public boolean supportsMultiblock() {
        return false;
    }

    /**
     * This class inherits vector steering from the ion thruster, but its propulsion is fuel-based.
     * Do not inherit IonThrusterBlockEntity's empty-fluid and FE-only visual predicates.
     */
    @Override
    public FluidStack fluidStack() {
        return tank == null ? FluidStack.EMPTY : tank.getPrimaryHandler().getFluid();
    }

    @Override
    public boolean validFluid() {
        FluidStack fuel = fluidStack();
        return !fuel.isEmpty() && getFuelProperties(fuel.getFluid()) != null;
    }

    @Override
    protected boolean isWorking() {
        return validFluid();
    }

    @Override
    public boolean isIon() {
        return false;
    }

    @Override
    public boolean isVisuallyActive() {
        return getThrottle() > 0.0d && validFluid();
    }

    @Override
    public IFluidHandler getFluidHandler(Direction side) {
        if (tank == null || (side != null && side != getFluidCapSide())) {
            return null;
        }
        return tank.getPrimaryHandler();
    }

    @Override
    public int getFuelAmountMb() {
        return tank == null ? 0 : tank.getPrimaryHandler().getFluidAmount();
    }

    @Override
    public int getFuelCapacityMb() {
        return tank == null ? getBaseTankCapacityMb() : tank.getPrimaryHandler().getTankCapacity(0);
    }

    @Override
    protected net.createmod.catnip.lang.LangBuilder getGoggleStatus() {
        if (fluidStack().isEmpty()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.no_fuel"))
                .style(ChatFormatting.RED);
        }
        if (!validFluid()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.wrong_fuel"))
                .style(ChatFormatting.RED);
        }
        if (!isPowered()) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.not_powered"))
                .style(ChatFormatting.GOLD);
        }
        if (getEmptyBlocks() == 0) {
            return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed"))
                .style(ChatFormatting.RED);
        }
        return CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status.working"))
            .style(ChatFormatting.GREEN);
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        // Do not call IonThrusterBlockEntity's implementation: it appends FE storage
        // and FE/t consumption. Use its shared output section, then show liquid fuel
        // in the same format as the regular Thruster.
        addIonThrusterOutputDetails(tooltip);
        if (tank == null) {
            return;
        }

        IFluidHandler handler = tank.getPrimaryHandler();
        int capacity = handler.getTankCapacity(0);
        int amount = handler.getFluidInTank(0).getAmount();

        CreateLang.builder()
            .add(Component.translatable("createpropulsion.gui.goggles.thruster.fuel_label"))
            .style(ChatFormatting.WHITE)
            .forGoggles(tooltip);
        CreateLang.builder()
            .add(Component.literal("  "))
            .add(Component.literal(Integer.toString(amount)).withStyle(ChatFormatting.AQUA))
            .add(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
            .add(Component.literal(Integer.toString(capacity)).withStyle(ChatFormatting.AQUA))
            .add(Component.literal(" mB").withStyle(ChatFormatting.GRAY))
            .forGoggles(tooltip);
        CreateLang.builder()
            .add(Component.literal("  "))
            .add(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", lastConsumedMbPerTick)).withStyle(ChatFormatting.AQUA))
            .add(Component.literal(" mB/t").withStyle(ChatFormatting.GRAY))
            .forGoggles(tooltip);
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        updateSingleThrust(currentBlockState);
    }

    /** Shared plume resolver access to the existing fuel-driven particle selection. */
    public net.minecraft.core.particles.ParticleOptions createResolvedParticleOptions() {
        var properties = getFuelProperties(fluidStack().getFluid());
        return properties == null
                ? new dev.propulsionteam.propulsionsimulated.particles.plume.PlumeParticleData()
                : properties.particleType().createParticleOptions(properties);
    }

    private void onVectorSignalChanged() {
        updateMappedTargets();
        if (level != null && !level.isClientSide) {
            setChanged();
            notifyUpdate();
        }
    }

    private void updateMappedTargets() {
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
    public double getNozzleOffsetFromCenter() {
        return PropulsionConfig.NOZZLE_OFFSET_FROM_CENTER.get();
    }

    @Override
    protected double getBaseThrust() {
        return PropulsionConfig.getLiquidVectorThrusterBaseThrustOrDefault();
    }

    @Override
    protected double getRawThrustCap() {
        return PropulsionConfig.getLiquidVectorThrusterBaseThrustOrDefault();
    }

    @Override
    protected int getBaseTankCapacityMb() {
        return PropulsionConfig.getLiquidVectorThrusterFuelTankCapacityMbOrDefault();
    }

    @Override
    protected double getFuelConsumptionPerTickAtFullThrottle() {
        return PropulsionConfig.getLiquidVectorThrusterFuelMbPerTickAtFullThrottleOrDefault();
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

    private static class VectorThrusterLinkTransform extends ValueBoxTransform.Dual {
        private final Direction localSide;

        public VectorThrusterLinkTransform(boolean first, Direction localSide) {
            super(first);
            this.localSide = localSide;
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Vec3 local = switch (localSide) {
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
                case EAST  -> VecHelper.rotate(vec, -90, Direction.Axis.Y);
                case SOUTH -> VecHelper.rotate(vec, 180, Direction.Axis.Y);
                case WEST  -> VecHelper.rotate(vec, 90, Direction.Axis.Y);
                case UP    -> VecHelper.rotate(vec, 90, Direction.Axis.X);
                case DOWN  -> VecHelper.rotate(vec, -90, Direction.Axis.X);
            };
        }
    }
}
