package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerBehaviour;
import dev.propulsionteam.propulsionsimulated.particles.plume.PlumeParticleData;
import dev.propulsionteam.propulsionsimulated.particles.ion.IonParticleData;
import dev.propulsionteam.propulsionsimulated.particles.plasma.PlasmaParticleData;
import dev.propulsionteam.propulsionsimulated.utility.GoggleUtils;
import dev.propulsionteam.propulsionsimulated.utility.math.MathUtility;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Math;
import org.joml.Vector3d;

import java.util.List;
import java.util.Locale;

public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity
    implements IHaveGoggleInformation, dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor, BlockSubLevelAssemblyListener {
    // ThrusterData thrust is stored in pN-like units where `thrustUnitsPerKn` units == 1 displayed kN.
    // Sable impulse conversion must use the same basis so display/diagram and applied force match.
    protected static final double PARTICLE_BROADCAST_RANGE_BLOCKS = 150.0d;
    //Constants
    protected static final int TICKS_PER_ENTITY_CHECK = 5;
    public static final int STARTUP_DURATION_TICKS = 10;
    protected static final float PARTICLE_VELOCITY = 4.0f;
    /** Used by server emit logic and client preview so plume segments stay visually continuous. */
    public static final double TARGET_PARTICLE_SPACING_BLOCKS = 0.5d;
    /** Matches {@link PropulsionConfig} thruster particle multiplier defineInRange max (0–32). */
    protected static final double PARTICLE_MULTIPLIER_CAP = 32.0d;
    protected static final double OBSTRUCTION_RAY_START_EPSILON = 0.05d;
    
    protected static final float LOWEST_POWER_THRESHOLD = 5.0f / 15.0f;

    //Common State
    protected ThrusterData thrusterData;
    @javax.annotation.Nullable
    protected BlockPos controllerPos;
    public int width = 1;
    protected String dyeId = null;
    protected int emptyBlocks;
    protected boolean isThrustDirty = false;
    protected boolean isDirty = false;
    @javax.annotation.Nullable
    private Vec3 previousParticleNozzleWorld;
    @javax.annotation.Nullable
    private Vec3 previousParticleExhaustWorld;
    private Vec3 previousParticleNozzleVelocity = Vec3.ZERO;
    private double particleEmissionCarry;
    private float adaptiveTrailCoverage;
    private boolean adaptiveTrailActive;
    private int adaptiveTrailQuietTicks;

    //Ticking
    private int currentTick = 0;
    private int startupTicks = 0;
    private boolean wasOperational = false;
    private float fadePower = 0.0f;
    private long lastThrustUpdateGameTime = -1L;
    private int thrustUpdateIntervalTicks = 10;

    protected double getParticleBroadcastRange() { return PARTICLE_BROADCAST_RANGE_BLOCKS; }
    protected float getParticleVelocity() { return PARTICLE_VELOCITY; }
    protected double getThrustUnitsPerKn() { return PropulsionConfig.getThrustUnitsPerKnOrDefault(); }

    protected double getParticleCountMultiplier() {
        return 1.0;
    }

    protected double getParticleVelocityMultiplier() {
        return 1.0;
    }

    /** First-tick visual exhaust displacement used by the adaptive client trail. */
    public double getParticleTrailInitialStep() {
        int multiblockWidth = Math.max(1, width);
        double multiblockVelocityScale = 1.0d + 0.30d * (multiblockWidth - 1);
        return getParticleVelocity() * Math.max(getEffectiveThrottle(), MathUtility.epsilon)
                * org.joml.Math.clamp(0.0d, PARTICLE_MULTIPLIER_CAP, getParticleVelocityMultiplier())
                * multiblockVelocityScale * 0.144d;
    }

    //CC Peripheral
    public AbstractComputerBehaviour computerBehaviour;
    public enum ControlMode {
        NORMAL,
        PERIPHERAL
    }

    protected ControlMode controlMode = ControlMode.NORMAL;
    protected int redstoneInput = 0;
    protected float digitalInput = 0.0f;

    public AbstractThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        thrusterData = new ThrusterData();
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide) {
            BlockState state = getBlockState();
            calculateObstruction(level, worldPosition, state.getValue(AbstractThrusterBlock.FACING));
            ThrusterData data = this.getThrusterData();
            data.setDirection(getThrustDirectionLocal());
            data.setThrust(0);

            Block block = getBlockState().getBlock();
            if (block instanceof AbstractThrusterBlock) {
                ((AbstractThrusterBlock) block).doRedstoneCheck(level, getBlockState(), worldPosition);
            }
        }
    }

    //Control logic

    public void setRedstoneInput(int power) {
        if (redstoneInput != power) {
            redstoneInput = power;
            if (controlMode == ControlMode.NORMAL) {
                dirtyThrust();
                isDirty = true;
            }
        }
    }

    public void setDigitalInput(float power) {
        float clamped = org.joml.Math.clamp(0.0f, 1.0f, power);
        if (java.lang.Math.abs(digitalInput - clamped) > 1e-4) {
            digitalInput = clamped;
            if (controlMode == ControlMode.PERIPHERAL) {
                dirtyThrust();
                isDirty = true;
            }
        }
    }

    public void setControlMode(ControlMode mode) {
        if (this.controlMode != mode) {
            this.controlMode = mode;
            dirtyThrust();
            isDirty = true;
        }
    }

    public float getPower() {
        if (controlMode == ControlMode.PERIPHERAL) {
            return digitalInput;
        }
        return redstoneInput / 15.0f;
    }

    public int getLegacyPowerInt() {
        return (int) Math.round(getPower() * 15);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (PropulsionCompatibility.CC_ACTIVE) {
            ComputerBehaviour behaviour = ComputerBehaviour.tryCreate(this);
            if (behaviour != null) {
                behaviours.add(computerBehaviour = behaviour);
            }
        }
        behaviours.add(new ThrusterDamager(this));
    }

    @Override
    public void tick() {
        if (this.isRemoved()) {
            return;
        }

        if (isDirty) {
            notifyUpdate();
            isDirty = false;
        }

        // Fix: Do not check block state when outside build height to prevent self-removal.
        // Minecraft returns VOID_AIR outside build limits, which causes the check to fail.
        boolean isOutsideWorldHeight = SimulatedThrustAdapter.isOutsideWorldBuildHeight(level, worldPosition);
        if (!isOutsideWorldHeight) {
            if (SimulatedThrustAdapter.getBlockStateSafe(level,worldPosition).getBlock() != getBlockState().getBlock()) {
                this.setRemoved();
                return;
            }
        }

        super.tick();
        thrusterData.setDirection(getThrustDirectionLocal());
        BlockState currentBlockState = isOutsideWorldHeight ? getBlockState() : SimulatedThrustAdapter.getBlockStateSafe(level,worldPosition);
        if (level.isClientSide) {
            ThrusterSoundHooks.clientTick(this);
            return;
        }

        boolean startupChanged = updateStartupState();
        if (!PropulsionConfig.useShaderPlumes()) {
            emitResolvedParticles(level, worldPosition, currentBlockState);
        }

        currentTick++;
        final int tick_rate = 10;

        //Periodically recalculate obstruction
        if (currentTick % (tick_rate * 2) == 0) {
            int previousEmptyBlocks = emptyBlocks;
            calculateObstruction(level, worldPosition, currentBlockState.getValue(AbstractThrusterBlock.FACING));
            if (previousEmptyBlocks != emptyBlocks) {
                isThrustDirty = true;
                setChanged();
                level.sendBlockUpdated(worldPosition, currentBlockState, currentBlockState, Block.UPDATE_CLIENTS);
            }
        }

        //Update thrust periodically or when marked dirty
        if (startupChanged || isThrustDirty || currentTick % tick_rate == 0) {
            updateThrustUpdateInterval();
            updateThrust(currentBlockState);
        }
    }

    private boolean updateStartupState() {
        boolean hasPowerCommand = getPower() > MathUtility.epsilon;
        boolean operational = isController()
                && hasPowerCommand
                && isWorking()
                && emptyBlocks > 0;
        int previousTicks = startupTicks;
        boolean previousOperational = wasOperational;
        float previousFadePower = fadePower;

        if (operational) {
            fadePower = getPower();
            if (wasOperational && startupTicks < STARTUP_DURATION_TICKS) {
                startupTicks++;
            }
        } else if (!hasPowerCommand && previousOperational) {
            // Preserve the exact output at the power-off edge. Subsequent ticks fade it down.
        } else if (!hasPowerCommand && startupTicks > 0 && getCurrentThrust() > MathUtility.epsilon) {
            startupTicks--;
            if (startupTicks == 0) fadePower = 0.0f;
        } else {
            // Fuel/energy loss or a blocked exhaust cannot sustain a shutdown burn.
            startupTicks = 0;
            fadePower = 0.0f;
        }
        wasOperational = operational;

        boolean changed = previousTicks != startupTicks
                || previousOperational != operational
                || java.lang.Math.abs(previousFadePower - fadePower) > 1.0e-4f;
        if (changed) {
            isThrustDirty = true;
            setChanged();
            notifyUpdate();
        }
        return changed;
    }

    private void updateThrustUpdateInterval() {
        if (level == null) {
            thrustUpdateIntervalTicks = isStartingUp() ? 1 : 10;
            return;
        }
        long gameTime = level.getGameTime();
        if (lastThrustUpdateGameTime < 0L) {
            thrustUpdateIntervalTicks = isStartingUp() ? 1 : 10;
        } else {
            thrustUpdateIntervalTicks = (int) java.lang.Math.max(1L, gameTime - lastThrustUpdateGameTime);
        }
        lastThrustUpdateGameTime = gameTime;
    }

    public abstract void updateThrust(BlockState currentBlockState);

    protected abstract boolean isWorking();

    protected abstract LangBuilder getGoggleStatus();

    public ThrusterData getThrusterData() {
        return thrusterData;
    }

    protected void setThrustAndSync(float thrust) {
        float previousThrust = (float) thrusterData.getThrust();
        thrusterData.setThrust(thrust);
        if (level == null || level.isClientSide) {
            return;
        }
        if (java.lang.Math.abs(previousThrust - thrust) < 0.01f) {
            return;
        }
        setChanged();
        notifyUpdate();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    public int getEmptyBlocks() {
        return emptyBlocks;
    }

    public void dirtyThrust() {
        isThrustDirty = true;
    }

    public boolean shouldEmitParticles() {
        return !PropulsionConfig.useShaderPlumes() && shouldEmitPlume();
    }

    public boolean shouldEmitPlume() {
        return isVisuallyActive();
    }

    public boolean shouldRenderShaderPlume() {
        return PropulsionConfig.useShaderPlumes() && shouldEmitPlume();
    }

    protected boolean supportsMultiblock() {
        return false;
    }

    public boolean isMultiblock() {
        return width > 1;
    }

    public boolean isController() {
        return controllerPos == null;
    }

    protected boolean shouldDamageEntities() {
        return PropulsionConfig.DAMAGE_ENTITIES.get() && getCurrentThrust() > MathUtility.epsilon;
    }

    protected void addSpecificGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {}
    
    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(AbstractThrusterBlock.FACING)) {
            return state.getValue(AbstractThrusterBlock.FACING);
        }
        return Direction.NORTH;
    }

    public float getCurrentThrust() {
        return (float) thrusterData.getThrust();
    }

    public float getStartupProgress() {
        return org.joml.Math.clamp(0.0f, 1.0f, (float) startupTicks / (float) STARTUP_DURATION_TICKS);
    }

    public boolean isStartingUp() {
        return wasOperational && startupTicks < STARTUP_DURATION_TICKS;
    }

    public boolean isFadingOut() {
        return !wasOperational && startupTicks > 0 && fadePower > MathUtility.epsilon;
    }

    private float getEnvelopePower() {
        return wasOperational ? getPower() : fadePower;
    }

    public float getEffectiveThrottle() {
        return getEnvelopePower() * getStartupProgress();
    }

    /** Actual output fraction after obstruction and startup are both accounted for. */
    public float getEffectiveThrustPercentage() {
        return Math.min(getEnvelopePower(), calculateObstructionEffect()) * getStartupProgress();
    }

    protected int getThrustUpdateIntervalTicks() {
        return thrustUpdateIntervalTicks;
    }

    public boolean isVisuallyActive() {
        return getEffectiveThrottle() > MathUtility.epsilon && (isWorking() || isFadingOut());
    }

    public int getUnobstructedBlocks() {
        return emptyBlocks;
    }

    public double getDisplayedThrustPnForTooltip() {
        return thrusterData.getThrust();
    }

    public double getDisplayedAirflowMsForTooltip() {
        return getEffectiveThrottle() * calculateObstructionEffect() * 200.0;
    }
    protected float getFuelEfficiencyMultiplier() { return 1.0f; }
    
    public boolean isCreative() { return false; }
    public boolean isIon() { return false; }

    public String getDyeId() { return dyeId; }

    public void setDyeId(String id) {
        this.dyeId = id;
        setChanged();
        notifyUpdate();
    }

    public Integer getDyeColor() {
        return dyeId != null ? PropulsionConfig.getDyeColor(dyeId) : null;
    }
    
    public dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity.PlumeType getPlumeType() {
        return dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity.PlumeType.NONE;
    }
    
    public IFluidHandler getFluidHandler(Direction side) { return null; }

    protected boolean isPowered() {
        return getPower() > MathUtility.epsilon;
    }

    protected float calculateObstructionEffect() {
        return (float) emptyBlocks / (float) PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
    }

    protected ParticleOptions createParticleOptions() {
        return new PlumeParticleData();
    }

    public abstract double getNozzleOffsetFromCenter();
    protected abstract double getBaseThrust();
    protected abstract double getRawThrustCap();

    /**
     * Returns a multiplier that models atmospheric losses by altitude.
     * The effect is configurable and never hard-cuts thrust to zero.
     */
    protected double calculateAtmosphericFactor() {
        if (!PropulsionConfig.USE_ATMOSPHERIC_PRESSURE.get()) return 1.0;
        Level lvl = getLevel();
        if (lvl == null) return 1.0;

        Vec3 worldPos = Sable.HELPER.projectOutOfSubLevel(lvl, Vec3.atCenterOf(worldPosition));
        double y = worldPos.y;

        double sea = lvl.getSeaLevel();
        double worldTop = lvl.getMaxBuildHeight();
        double normalizedAltitude = 0.0d;
        if (worldTop > sea + MathUtility.epsilon) {
            normalizedAltitude = org.joml.Math.clamp(0.0d, 1.0d, (y - sea) / (worldTop - sea));
        }
        
        // Proxy for air pressure (1.0 at sea level, 0.0 at space/build limit)
        double airPressure = 1.0 - normalizedAltitude;
        double strength = org.joml.Math.clamp(0.0d, 2.0d, PropulsionConfig.ATMOSPHERIC_PRESSURE_AMOUNT.get());

        if (this.isIon()) {
            // Ion propulsion suffers strongly in dense air and ramps up toward vacuum.
            // 1.0 pressure -> ~20% thrust, near-vacuum -> ~100%.
            double target = org.joml.Math.clamp(0.2d, 1.0d, 1.0d - 0.8d * airPressure);
            return org.joml.Math.clamp(0.05d, 5.0d, 1.0d + (target - 1.0d) * strength);
        }

        // Chemical/rocket thrusters stay mostly constant; altitude gives a mild bonus.
        double vacuumBonus = airPressure < 1.0d ? (1.0d - airPressure) * 0.15d : 0.0d;
        double target = 1.0d + vacuumBonus;
        return org.joml.Math.clamp(0.05d, 5.0d, 1.0d + (target - 1.0d) * strength);
    }

    public float getThrottle() {
        return getPower();
    }

    public Vector3d getThrustDirectionLocal() {
        Direction facing = getFacing();
        return new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ()).normalize();
    }

    protected Vec3 getParticleExhaustDirectionLocal() {
        Vector3d localThrustDirection = getThrustDirectionLocal();
        return new Vec3(-localThrustDirection.x, -localThrustDirection.y, -localThrustDirection.z);
    }

    public Vec3 getParticleDebugExhaustDirectionLocal() {
        Vec3 localExhaustDirection = getParticleExhaustDirectionLocal();
        if (localExhaustDirection.lengthSqr() < MathUtility.epsilon) {
            Direction oppositeDirection = getFacing().getOpposite();
            localExhaustDirection = new Vec3(oppositeDirection.getStepX(), oppositeDirection.getStepY(), oppositeDirection.getStepZ());
        } else {
            localExhaustDirection = localExhaustDirection.normalize();
        }
        return localExhaustDirection;
    }

    protected Vec3 getLocalNozzlePosition(BlockPos pos, Vec3 localExhaustDirection, double nozzleOffset) {
        Vec3 localBlockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return localBlockCenter.add(localExhaustDirection.scale(nozzleOffset));
    }

    public Vec3 getParticleDebugNozzlePositionLocal() {
        Vec3 localExhaustDirection = getParticleDebugExhaustDirectionLocal();
        double currentNozzleOffset = getNozzleOffsetFromCenter();
        return getLocalNozzlePosition(worldPosition, localExhaustDirection, currentNozzleOffset);
    }

    public record WorldExhaustRay(Level level, Vec3 nozzlePos, Vec3 direction) {}

    public record ObstructionRaySample(double firstHitDistance, int emptyBlocksEstimate) {}

    public WorldExhaustRay getWorldExhaustRay() {
        if (level == null) {
            return null;
        }
        Vec3 localNozzle = getParticleDebugNozzlePositionLocal();
        Vec3 localExhaust = getParticleDebugExhaustDirectionLocal();

        Vector3d localNozzleVec = new Vector3d(localNozzle.x, localNozzle.y, localNozzle.z);
        Vector3d localExhaustVec = new Vector3d(localExhaust.x, localExhaust.y, localExhaust.z);
        if (localExhaustVec.lengthSquared() < MathUtility.epsilon) {
            Direction opposite = getFacing().getOpposite();
            localExhaustVec.set(opposite.getStepX(), opposite.getStepY(), opposite.getStepZ());
        }
        localExhaustVec.normalize();

        SimulatedThrustAdapter.Projection projection = SimulatedThrustAdapter.projectToWorld(level, worldPosition, localNozzleVec, localExhaustVec);
        Vec3 worldDirection = projection.direction();
        if (worldDirection.lengthSqr() < MathUtility.epsilon) {
            worldDirection = new Vec3(localExhaustVec.x, localExhaustVec.y, localExhaustVec.z);
        } else {
            worldDirection = worldDirection.normalize();
        }
        return new WorldExhaustRay(projection.level(), projection.position(), worldDirection);
    }

    @Override
    public AABB getRenderBoundingBox() {
        int w = Math.max(1, width);
        double extra = 32.0d;

        return new AABB(
                worldPosition.getX() - extra,
                worldPosition.getY() - extra,
                worldPosition.getZ() - extra,
                worldPosition.getX() + w + extra,
                worldPosition.getY() + w + extra,
                worldPosition.getZ() + w + extra
        );
    }

    protected ObstructionRaySample sampleObstructionRaycast(Level level, int scanLength) {
        if (scanLength <= 0) {
            return new ObstructionRaySample(0.0d, 0);
        }

        // When the config option is enabled and the thruster is on a sub-level, clip in
        // local sub-level space so only blocks belonging to the same sub-level count.
        final dev.ryanhcode.sable.sublevel.SubLevel containingSubLevel = Sable.HELPER.getContaining(level, worldPosition);
        if (PropulsionConfig.OBSTRUCTION_IGNORE_OTHER_SUBLEVELS.get()
                && containingSubLevel != null) {
            Vec3 localNozzle = getParticleDebugNozzlePositionLocal();
            Vec3 localDir = getParticleDebugExhaustDirectionLocal();
            Vec3 rayStart = localNozzle.add(localDir.scale(OBSTRUCTION_RAY_START_EPSILON));
            Vec3 rayEnd = rayStart.add(localDir.scale(scanLength));

            ClipContext clipContext = new ClipContext(
                rayStart,
                rayEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()
            );
            BlockHitResult hitResult = level.clip(clipContext);

            double firstHitDistance = scanLength;
            boolean hit = hitResult.getType() == BlockHitResult.Type.BLOCK;
            if (hit) {
                firstHitDistance = Math.min(scanLength, rayStart.distanceTo(hitResult.getLocation()));
            }
            int emptyBlocksEstimate = hit
                ? Math.clamp((int) java.lang.Math.floor(firstHitDistance), 0, scanLength)
                : scanLength;
            return new ObstructionRaySample(firstHitDistance, emptyBlocksEstimate);
        }

        WorldExhaustRay worldRay = getWorldExhaustRay();
        if (worldRay == null) {
            return new ObstructionRaySample(0.0d, 0);
        }

        Vec3 rayStart = worldRay.nozzlePos().add(worldRay.direction().scale(OBSTRUCTION_RAY_START_EPSILON));
        Vec3 rayEnd = rayStart.add(worldRay.direction().scale(scanLength));

        ClipContext clipContext = new ClipContext(
            rayStart,
            rayEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            net.minecraft.world.phys.shapes.CollisionContext.empty()
        );
        BlockHitResult hitResult = worldRay.level().clip(clipContext);

        double firstHitDistance = scanLength;
        boolean hit = hitResult.getType() == BlockHitResult.Type.BLOCK;
        if (hit) {
            firstHitDistance = Math.min(scanLength, rayStart.distanceTo(hitResult.getLocation()));
        }

        int emptyBlocksEstimate = hit
            ? Math.clamp((int) java.lang.Math.floor(firstHitDistance), 0, scanLength)
            : scanLength;
        return new ObstructionRaySample(firstHitDistance, emptyBlocksEstimate);
    }

    public void setRedstonePower(int power) {
        setRedstoneInput(power);
    }

    public boolean tryConsumeFuelBucket(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack heldStack) {
        return false;
    }

    public int getFuelAmountMb() { return 0; }
    public int getFuelCapacityMb() { return 0; }
    public boolean validFluid() { return false; }
    public net.neoforged.neoforge.fluids.FluidStack fluidStack() { return net.neoforged.neoforge.fluids.FluidStack.EMPTY; }

    public boolean isActive() {
        return (isPowered() && isWorking()) || isFadingOut();
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        // Resource loss is immediate, while a commanded shutdown keeps applying the
        // explicitly tracked fade envelope until it reaches zero.
        if (!this.isActive() || this.getCurrentThrust() <= 0.0d) {
            return;
        }

        final dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterForceProvider.ForceSample sample = 
            dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterForceProvider.createSample(this, timeStep);
            
        double scaledThrust = this.getCurrentThrust();
        if (scaledThrust <= 0.0d || !Double.isFinite(scaledThrust)) {
            return;
        }

        Vector3d adjustedImpulse = new Vector3d(sample.impulseLocal()).div(getThrustUnitsPerKn());
        SimulatedThrustAdapter.applyImpulseAtPoint(subLevel, sample.pointLocal(), adjustedImpulse);
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        // Recompute obstruction and refresh redstone-derived power after assembly/disassembly moves.
        previousParticleNozzleWorld = null;
        previousParticleExhaustWorld = null;
        previousParticleNozzleVelocity = Vec3.ZERO;
        particleEmissionCarry = 0.0d;
        adaptiveTrailCoverage = 0.0f;
        adaptiveTrailActive = false;
        adaptiveTrailQuietTicks = 0;
        if (newLevel != null) {
            setRedstoneInput(newLevel.getBestNeighborSignal(newPos));
            calculateObstruction(newLevel, newPos, state.getValue(AbstractThrusterBlock.FACING));
            dirtyThrust();
        }
    }

    /** Compatibility entrypoint; base ticking uses the non-overridable resolved path below. */
    public void emitParticles(Level level, BlockPos pos, BlockState state) {
        emitResolvedParticles(level, pos, state);
    }

    private void emitResolvedParticles(Level level, BlockPos pos, BlockState state) {
        // Sample the nozzle every tick, including while idle. This avoids a stale accumulated
        // displacement when a moving thruster is switched back on.
        Vec3 localNozzlePosition = getParticleDebugNozzlePositionLocal();
        Vec3 worldNozzlePosition = Sable.HELPER.projectOutOfSubLevel(level, localNozzlePosition);
        Vec3 finiteDifferenceVelocity = previousParticleNozzleWorld == null
            ? Vec3.ZERO
            : worldNozzlePosition.subtract(previousParticleNozzleWorld);

        Vec3 localExhaustDirection = getParticleDebugExhaustDirectionLocal();
        Vec3 worldAheadPosition = Sable.HELPER.projectOutOfSubLevel(level, localNozzlePosition.add(localExhaustDirection));
        Vec3 worldExhaustDirection = worldAheadPosition.subtract(worldNozzlePosition);
        if (worldExhaustDirection.lengthSqr() < MathUtility.epsilon) {
            worldExhaustDirection = localExhaustDirection.normalize();
        } else {
            worldExhaustDirection = worldExhaustDirection.normalize();
        }

        Vec3 nozzleVelocity = sampleNozzleVelocity(level, localNozzlePosition, finiteDifferenceVelocity);
        Vec3 oldNozzlePosition = previousParticleNozzleWorld;
        Vec3 oldExhaustDirection = previousParticleExhaustWorld;
        Vec3 oldNozzleVelocity = previousParticleNozzleVelocity;

        double angleChange = oldExhaustDirection == null ? 0.0d
                : PlumeTrailMath.angleDegrees(oldExhaustDirection, worldExhaustDirection);
        double reconstructedGap = oldNozzlePosition == null ? 0.0d
                : worldNozzlePosition.distanceTo(oldNozzlePosition.add(oldNozzleVelocity));
        updateAdaptiveTrailCoverage(angleChange, reconstructedGap);

        previousParticleNozzleWorld = worldNozzlePosition;
        previousParticleExhaustWorld = worldExhaustDirection;
        previousParticleNozzleVelocity = nozzleVelocity;

        ThrusterPlumeSpec plume = ThrusterPlumeResolver.resolve(this);
        if (!plume.active() || plume.particle() == null) return;
        if (emptyBlocks == 0) return;
        float power = getEffectiveThrottle();
        if (power <= MathUtility.epsilon) return;

        double particleCountMultiplier = org.joml.Math.clamp(0.0d, PARTICLE_MULTIPLIER_CAP, getParticleCountMultiplier());
        if (particleCountMultiplier <= 0) return;
        double particleVelocityMultiplier = org.joml.Math.clamp(0.0d, PARTICLE_MULTIPLIER_CAP, getParticleVelocityMultiplier());
        int multiblockWidth = Math.max(1, width);
        double multiblockVelocityScale = 1.0d + 0.30d * (multiblockWidth - 1);

        float emissionScale = (float) Math.max(power, MathUtility.epsilon);

        // The particle implementation applies its own speed multiplier and drag to this
        // exhaust component. Nozzle motion is carried separately in the particle data so it
        // remains in world units and the moving craft cannot overtake the plume.
        Vector3d particleVelocity = new Vector3d(worldExhaustDirection.x, worldExhaustDirection.y, worldExhaustDirection.z)
            .mul(getParticleVelocity() * emissionScale * particleVelocityMultiplier * multiblockVelocityScale);

        // Enough particles each tick so spacing along the velocity vector stays near TARGET_PARTICLE_SPACING_BLOCKS (no fractional carry → no skipped ticks).
        double speedPerTick = particleVelocity.length();
        ParticleOptions baseParticleData = withStartupProgress(plume.particle(), getStartupProgress());
        double startupDensityScale = startupDensityScale(baseParticleData);
        double density = speedPerTick / TARGET_PARTICLE_SPACING_BLOCKS
                * particleCountMultiplier * multiblockWidth * startupDensityScale;
        int particleCap = Math.max(0, PropulsionConfig.CLIENT_PARTICLES_PER_TICK.get());
        PlumeTrailMath.EmissionBudget emissionBudget = PlumeTrailMath.emissionBudget(
                density, particleEmissionCarry, particleCap);
        int particlesToSpawn = emissionBudget.count();
        particleEmissionCarry = emissionBudget.carry();

        if (particlesToSpawn == 0) return;

        for (int i = 0; i < particlesToSpawn; i++) {
            double sampleT = oldNozzlePosition == null || oldExhaustDirection == null
                    ? 1.0d
                    : (i + 0.5d) / particlesToSpawn;
            Vec3 sampleDirection = oldExhaustDirection == null
                    ? worldExhaustDirection
                    : PlumeTrailMath.slerpDirection(oldExhaustDirection, worldExhaustDirection, sampleT);
            Vec3 sampleVelocity = oldNozzlePosition == null
                    ? nozzleVelocity
                    : oldNozzleVelocity.lerp(nozzleVelocity, sampleT);
            Vec3 samplePosition = oldNozzlePosition == null
                    ? worldNozzlePosition
                    : PlumeTrailMath.hermite(oldNozzlePosition, oldNozzleVelocity,
                            worldNozzlePosition, nozzleVelocity, sampleT);

            // Advance historical sub-tick samples to the current tick. Unlike the removed
            // straight pre-spread, every sample uses its own interpolated orientation.
            double sampleAge = 1.0d - sampleT;
            double visualExhaustStep = speedPerTick * 0.144d;
            samplePosition = samplePosition.add(sampleVelocity.scale(sampleAge))
                    .add(sampleDirection.scale(visualExhaustStep * sampleAge));

            ParticleOptions particleData = withMotion(baseParticleData, sampleVelocity, adaptiveTrailCoverage);
            Vector3d sampleParticleVelocity = new Vector3d(sampleDirection.x, sampleDirection.y, sampleDirection.z)
                    .mul(speedPerTick);
            double spawnX = samplePosition.x;
            double spawnY = samplePosition.y;
            double spawnZ = samplePosition.z;

            if (level instanceof ServerLevel serverLevel) {
                double maxDistSq = getParticleBroadcastRange() * getParticleBroadcastRange();
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(spawnX, spawnY, spawnZ) > maxDistSq) {
                        continue;
                    }
                    serverLevel.sendParticles(
                        player,
                        particleData,
                        true,
                        spawnX, spawnY, spawnZ,
                        0,
                        sampleParticleVelocity.x, sampleParticleVelocity.y, sampleParticleVelocity.z,
                        1.0
                    );
                }
            } else {
                level.addParticle(
                    particleData,
                    true,
                    spawnX, spawnY, spawnZ,
                    sampleParticleVelocity.x, sampleParticleVelocity.y, sampleParticleVelocity.z
                );
            }
        }
    }

    private ParticleOptions withStartupProgress(ParticleOptions particle, float progress) {
        if (!isStartingUp()) return particle;
        if (particle instanceof PlumeParticleData plume) {
            return new PlumeParticleData(plume.overrideTextures(), plume.overrideColor(), plume.overrideSize(), progress,
                    plume.inheritedVelocity(), plume.trailCoverage());
        }
        if (particle instanceof IonParticleData ion) {
            return new IonParticleData(ion.overrideTextures(), ion.overrideColor(), ion.overrideSize(), progress,
                    ion.inheritedVelocity(), ion.trailCoverage());
        }
        if (particle instanceof PlasmaParticleData plasma) {
            return new PlasmaParticleData(plasma.overrideTextures(), plasma.overrideColor(), plasma.overrideSize(), progress,
                    plasma.inheritedVelocity(), plasma.trailCoverage());
        }
        return particle;
    }

    private static ParticleOptions withMotion(ParticleOptions particle, Vec3 velocity, float trailCoverage) {
        if (particle instanceof PlumeParticleData plume) {
            return new PlumeParticleData(plume.overrideTextures(), plume.overrideColor(), plume.overrideSize(),
                    plume.startupProgress(), velocity, trailCoverage);
        }
        if (particle instanceof IonParticleData ion) {
            return new IonParticleData(ion.overrideTextures(), ion.overrideColor(), ion.overrideSize(),
                    ion.startupProgress(), velocity, trailCoverage);
        }
        if (particle instanceof PlasmaParticleData plasma) {
            return new PlasmaParticleData(plasma.overrideTextures(), plasma.overrideColor(), plasma.overrideSize(),
                    plasma.startupProgress(), velocity, trailCoverage);
        }
        return particle;
    }

    private Vec3 sampleNozzleVelocity(Level level, Vec3 localNozzlePosition, Vec3 fallback) {
        try {
            if (Sable.HELPER.getContaining(level, localNozzlePosition) != null) {
                Vec3 velocityPerSecond = Sable.HELPER.getVelocity(level, localNozzlePosition);
                if (Double.isFinite(velocityPerSecond.x)
                        && Double.isFinite(velocityPerSecond.y)
                        && Double.isFinite(velocityPerSecond.z)) {
                    return velocityPerSecond.scale(1.0d / 20.0d);
                }
            }
        } catch (RuntimeException ignored) {
            // Sublevel physics may not be ready during assembly/chunk transitions.
        }
        return fallback;
    }

    private void updateAdaptiveTrailCoverage(double angleChange, double reconstructedGap) {
        float target = PlumeTrailMath.activationTarget(angleChange, reconstructedGap, adaptiveTrailActive);
        if (target > 0.0f) {
            adaptiveTrailActive = true;
            adaptiveTrailQuietTicks = 0;
        } else if (adaptiveTrailActive && ++adaptiveTrailQuietTicks >= 4) {
            adaptiveTrailActive = false;
            adaptiveTrailQuietTicks = 0;
        }
        float resolvedTarget = adaptiveTrailActive ? Math.max(target, 0.35f) : 0.0f;
        float blend = resolvedTarget > adaptiveTrailCoverage ? 0.34f : 0.15f;
        adaptiveTrailCoverage += (resolvedTarget - adaptiveTrailCoverage) * blend;
        if (adaptiveTrailCoverage < 0.01f && !adaptiveTrailActive) adaptiveTrailCoverage = 0.0f;
    }

    private static double startupDensityScale(ParticleOptions particle) {
        if (particle instanceof PlumeParticleData plume && plume.startupProgress() != null) {
            return org.joml.Math.lerp(plume.startupProgress(), 2.8d, 1.0d);
        }
        if (particle instanceof PlasmaParticleData plasma && plasma.startupProgress() != null) {
            return org.joml.Math.lerp(plasma.startupProgress(), 2.0d, 1.0d);
        }
        return 1.0d;
    }

    @SuppressWarnings("deprecation") // i hate compilers let me use ts
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection){
        // Raycast in world space so sublevel thrusters correctly collide against real-world blocks.
        int oldEmptyBlocks = this.emptyBlocks;
        ObstructionRaySample sample = sampleObstructionRaycast(level, PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get());
        this.emptyBlocks = sample.emptyBlocksEstimate();
        if (oldEmptyBlocks != this.emptyBlocks) { //Only set dirty if it actually changed
            isThrustDirty = true;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean wasThrustDirty = isThrustDirty;
        calculateObstruction(getLevel(), worldPosition, getBlockState().getValue(AbstractThrusterBlock.FACING));
        isThrustDirty = wasThrustDirty;

        MutableComponent title = Component.translatable("createpropulsion.gui.goggles.title.thruster_stats").copy();
        if (width > 1) {
            title.append(Component.literal(" (" + width + "x" + width + "x" + width + ")").withStyle(ChatFormatting.GRAY));
        }
        CreateLang.builder().add(title).style(ChatFormatting.WHITE).forGoggles(tooltip);
        CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.status")).text(": ").add(getGoggleStatus()).forGoggles(tooltip);

        addThrusterDetails(tooltip, isPlayerSneaking);

        if (controlMode == ControlMode.PERIPHERAL) {
            CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.cc.peripheral_controlled")).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }

        return true;
    }

    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        float obstructionEfficiency = 100;
        ChatFormatting tooltipColor = ChatFormatting.GREEN;
        int scanLength = PropulsionConfig.OBSTRUCTION_SCAN_LENGTH.get();
        if (emptyBlocks < scanLength) {
            obstructionEfficiency = calculateObstructionEffect() * 100;
            tooltipColor = GoggleUtils.efficiencyColor(obstructionEfficiency);
            CreateLang.builder().add(Component.translatable("createpropulsion.gui.goggles.thruster.obstructed")).space().add(CreateLang.text(GoggleUtils.makeObstructionBar(emptyBlocks, scanLength))).style(tooltipColor).forGoggles(tooltip);
        }

        // Show efficiency based only on block obstruction (100 = no obstruction)
        CreateLang.builder()
            .add(Component.translatable("createpropulsion.gui.goggles.thruster.efficiency")).text(": ").add(CreateLang.number(obstructionEfficiency)).add(CreateLang.text("%"))
            .style(tooltipColor).forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.thruster.thrust_output"))
                .style(ChatFormatting.WHITE)
                .forGoggles(tooltip);

        CreateLang.builder()
            .add(Component.literal("  "))
            .add(Component.translatable("createpropulsion.tooltip.thrust1").withStyle(ChatFormatting.GRAY))
            .add(Component.literal(String.format(Locale.ROOT, "%.2f", this.getDisplayedThrustPnForTooltip() / getThrustUnitsPerKn())).withStyle(ChatFormatting.AQUA))
            .add(Component.literal(" pN").withStyle(ChatFormatting.GRAY))
            .forGoggles(tooltip);
    }


    @Override
    protected void write(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putInt("emptyBlocks", emptyBlocks);
        compound.putInt("currentTick", currentTick);
        compound.putInt("StartupTicks", startupTicks);
        compound.putBoolean("StartupOperational", wasOperational);
        compound.putFloat("StartupFadePower", fadePower);
        
        compound.putInt("RedstoneInput", redstoneInput);
        compound.putFloat("DigitalInput", digitalInput);
        compound.putInt("ControlMode", controlMode.ordinal());
        // Sync thrust to clients when sending client packets / updates
        compound.putFloat("Thrust", (float) thrusterData.getThrust());
        if (dyeId != null) {
            compound.putString("DyeId", dyeId);
        } else {
            compound.remove("DyeId");
        }
    }

    @Override
    protected void read(CompoundTag compound, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        emptyBlocks = compound.getInt("emptyBlocks");
        currentTick = compound.getInt("currentTick");
        startupTicks = Math.clamp(compound.getInt("StartupTicks"), 0, STARTUP_DURATION_TICKS);
        wasOperational = compound.getBoolean("StartupOperational");
        fadePower = org.joml.Math.clamp(0.0f, 1.0f, compound.getFloat("StartupFadePower"));
        lastThrustUpdateGameTime = -1L;

        redstoneInput = compound.getInt("RedstoneInput");
        digitalInput = compound.getFloat("DigitalInput");
        if (clientPacket && compound.contains("ControlMode")) {
            int ordinal = compound.getInt("ControlMode");
            controlMode = ordinal >= 0 && ordinal < ControlMode.values().length
                ? ControlMode.values()[ordinal]
                : ControlMode.NORMAL;
        } else if (!clientPacket) {
            // Peripheral attachments do not survive a server restart. Never restore a
            // stale ownership mode before ComputerCraft has reattached and run code.
            controlMode = ControlMode.NORMAL;
        }
        // Read thrust value from sync packets if present
        if (compound.contains("Thrust")) {
            thrusterData.setThrust(compound.getFloat("Thrust"));
        }
        dyeId = compound.contains("DyeId") ? compound.getString("DyeId") : null;
    }

}
