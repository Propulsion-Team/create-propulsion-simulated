package dev.propulsionteam.propulsionsimulated.content.thruster;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerBehaviour;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.particles.plume.PlumeParticleData;
import dev.propulsionteam.propulsionsimulated.utility.GoggleUtils;
import dev.propulsionteam.propulsionsimulated.utility.math.MathUtility;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Math;
import org.joml.Vector3d;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

public abstract class AbstractThrusterBlockEntity extends SmartBlockEntity
        implements IHaveGoggleInformation, dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor, BlockSubLevelAssemblyListener {
    // ThrusterData thrust is stored in pN-like units where `thrustUnitsPerKn` units == 1 displayed kN.
    // Sable impulse conversion must use the same basis so display/diagram and applied force match.
    protected static final double PARTICLE_BROADCAST_RANGE_BLOCKS = 150.0d;
    //Constants
    protected static final int TICKS_PER_ENTITY_CHECK = 5;
    protected static final float PARTICLE_VELOCITY = 4.0f;
    /**
     * Used by server emit logic and client preview so plume segments stay visually continuous.
     */
    public static final double TARGET_PARTICLE_SPACING_BLOCKS = 0.5d;
    /**
     * Matches {@link PropulsionConfig} thruster particle multiplier defineInRange max (0–32).
     */
    protected static final double PARTICLE_MULTIPLIER_CAP = 32.0d;
    protected static final double OBSTRUCTION_RAY_START_EPSILON = 0.05d;

    protected static final float LOWEST_POWER_THRESHOLD = 5.0f / 15.0f;

    //Common State
    protected ThrusterData thrusterData;
    public int width = 1;
    protected String dyeId = null;
    protected int emptyBlocks;
    protected boolean isThrustDirty = false;

    //Ticking
    private int currentTick = 0;

    protected double getParticleBroadcastRange() {
        return PARTICLE_BROADCAST_RANGE_BLOCKS;
    }

    protected float getParticleVelocity() {
        return PARTICLE_VELOCITY;
    }

    protected double getThrustUnitsPerKn() {
        return PropulsionConfig.getThrustUnitsPerKnOrDefault();
    }

    protected double getParticleCountMultiplier() {
        return 1.0;
    }

    protected double getParticleVelocityMultiplier() {
        return 1.0;
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
                notifyUpdate();
            }
        }
    }

    public void setDigitalInput(float power) {
        float clamped = org.joml.Math.clamp(0.0f, 1.0f, power);
        if (java.lang.Math.abs(digitalInput - clamped) > 1e-4) {
            digitalInput = clamped;
            if (controlMode == ControlMode.PERIPHERAL) {
                dirtyThrust();
                notifyUpdate();
            }
        }
    }

    public void setControlMode(ControlMode mode) {
        if (this.controlMode != mode) {
            this.controlMode = mode;
            dirtyThrust();
            notifyUpdate();
        }
    }

    public float getPower() {
        if (controlMode == ControlMode.PERIPHERAL) {
            return digitalInput;
        }
        return redstoneInput / 15.0f;
    }

    public float getThrottle() {
        return getPower();
    }

    protected LerpedFloat interpolatedPower = LerpedFloat.linear().chase(0, 0.01, LerpedFloat.Chaser.LINEAR);

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

    final int tick_rate = 10;

    @Override
    public void tick() {
        if (this.isRemoved()) {
            return;
        }

        // Fix: Do not check block state when outside build height to prevent self-removal.
        // Minecraft returns VOID_AIR outside build limits, which causes the check to fail.
        boolean isOutsideWorldHeight = SimulatedThrustAdapter.isOutsideWorldBuildHeight(level, worldPosition);
        if (!isOutsideWorldHeight) {
            if (SimulatedThrustAdapter.getBlockStateSafe(level, worldPosition).getBlock() != getBlockState().getBlock()) {
                this.setRemoved();
                return;
            }
        }

        super.tick();
        thrusterData.setDirection(getThrustDirectionLocal());
        BlockState currentBlockState = isOutsideWorldHeight ? getBlockState() : SimulatedThrustAdapter.getBlockStateSafe(level, worldPosition);
        if (level.isClientSide) {
            ThrusterSoundHooks.clientTick(this);
            interpolatedPower.updateChaseTarget(getPower());
            interpolatedPower.tickChaser();
            return;
        }

        if (shouldEmitPlume()) {
            if (isMeshedPlume()) emitMeshedParticles(level, worldPosition, currentBlockState);
            else emitPlumeParticles(level, worldPosition, currentBlockState);
        }

        currentTick++;
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
        if (isThrustDirty || currentTick % tick_rate == 0) {
            updateThrust(currentBlockState);
        }
    }

    /**
     *
     * @return if this thruster is a meshed plume or a particle only plume
     */
    public boolean isMeshedPlume() {
        return false;
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

    public boolean shouldEmitPlume() {
        return isPowered() && isWorking();
    }

    protected boolean shouldDamageEntities() {
        return PropulsionConfig.DAMAGE_ENTITIES.get() && isPowered() && isWorking();
    }

    protected void addSpecificGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking) {
    }

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

    public boolean isVisuallyActive() {
        return getThrottle() > 0 && isWorking();
    }

    public int getUnobstructedBlocks() {
        return emptyBlocks;
    }

    public double getDisplayedThrustPnForTooltip() {
        return thrusterData.getThrust();
    }

    public double getDisplayedAirflowMsForTooltip() {
        return getPower() * calculateObstructionEffect() * 200.0;
    }

    protected float getFuelEfficiencyMultiplier() {
        return 1.0f;
    }

    public boolean isCreative() {
        return false;
    }

    public boolean isIon() {
        return false;
    }

    public String getDyeId() {
        return dyeId;
    }

    public void setDyeId(String id) {
        this.dyeId = id;
        setChanged();
        notifyUpdate();
    }

    public Integer getDyeColor() {
        return dyeId != null ? PropulsionConfig.getDyeColor(dyeId) : null;
    }

    public CreativeThrusterBlockEntity.PlumeType getPlumeType() {
        return CreativeThrusterBlockEntity.PlumeType.NONE;
    }

    public boolean isBluePlume() {
        return getPlumeType() == CreativeThrusterBlockEntity.PlumeType.PLASMA ||
                getPlumeType() == CreativeThrusterBlockEntity.PlumeType.ION;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        return null;
    }

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

    public record WorldExhaustRay(Level level, Vec3 nozzlePos, Vec3 direction) {
    }

    public record ObstructionRaySample(double firstHitDistance, int emptyBlocksEstimate) {
    }

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

    public int getFuelAmountMb() {
        return 0;
    }

    public int getFuelCapacityMb() {
        return 0;
    }

    public boolean validFluid() {
        return false;
    }

    public net.neoforged.neoforge.fluids.FluidStack fluidStack() {
        return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
    }

    public boolean isActive() {
        return isPowered() && isWorking();
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        if (this.getCurrentThrust() <= 0.0d) {
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
        if (newLevel != null) {
            setRedstoneInput(newLevel.getBestNeighborSignal(newPos));
            calculateObstruction(newLevel, newPos, state.getValue(AbstractThrusterBlock.FACING));
            dirtyThrust();
        }
    }

    protected final Random particleRandom = new Random();

    public void emitMeshedParticles(Level level, BlockPos pos, BlockState state) {
        if (emptyBlocks == 0) return;
        float power = getPower();
        if (particleRandom.nextFloat() > power && particleRandom.nextBoolean()) return;

        float emissionScale = (float) Math.max(power, MathUtility.epsilon);
        Vec3 localExhaustDirection = getParticleDebugExhaustDirectionLocal();
        Vec3 localNozzlePosition = getParticleDebugNozzlePositionLocal();

        // Convert local sublevel coordinates into world-space coordinates so particles align
        // with ship rotation instead of sticking to global axes.
        Vec3 worldNozzlePosition = Sable.HELPER.projectOutOfSubLevel(level, localNozzlePosition);
        Vec3 worldAheadPosition = Sable.HELPER.projectOutOfSubLevel(level, localNozzlePosition.add(localExhaustDirection));
        Vec3 worldExhaustDirection = worldAheadPosition.subtract(worldNozzlePosition);
        if (worldExhaustDirection.lengthSqr() < MathUtility.epsilon) {
            worldExhaustDirection = localExhaustDirection;
        } else {
            worldExhaustDirection = worldExhaustDirection.normalize();
        }

        Vector3d particleVelocity = new Vector3d(worldExhaustDirection.x, worldExhaustDirection.y, worldExhaustDirection.z)
                .mul(getParticleVelocity() * emissionScale * 0.15);

        double spawnX = worldNozzlePosition.x;
        double spawnY = worldNozzlePosition.y;
        double spawnZ = worldNozzlePosition.z;
        for (int i = 0; i < 2; i++) {
            emitParticle(
                    spawnX + (particleRandom.nextFloat() - 0.5) * 1,
                    spawnY + (particleRandom.nextFloat() - 0.5) * 1,
                    spawnZ + (particleRandom.nextFloat() - 0.5) * 1,
                    particleVelocity, isBluePlume() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME);
        }

        if (power > 0.5) {
            spawnX += worldExhaustDirection.x * 2;
            spawnY += worldExhaustDirection.y * 2;
            spawnZ += worldExhaustDirection.z * 2;
            emitParticle(
                    spawnX + (particleRandom.nextFloat() - 0.5) * 1.5,
                    spawnY + (particleRandom.nextFloat() - 0.5) * 1.5,
                    spawnZ + (particleRandom.nextFloat() - 0.5) * 1.5,
                    particleVelocity, ParticleTypes.LARGE_SMOKE);
        }
    }

    public void emitPlumeParticles(Level level, BlockPos pos, BlockState state) {
        if (emptyBlocks == 0) return;
        float power = getPower();

        double particleCountMultiplier = org.joml.Math.clamp(0.0d, PARTICLE_MULTIPLIER_CAP, getParticleCountMultiplier());
        if (particleCountMultiplier <= 0) return;
        double particleVelocityMultiplier = org.joml.Math.clamp(0.0d, PARTICLE_MULTIPLIER_CAP, getParticleVelocityMultiplier());

        float emissionScale = (float) Math.max(power, MathUtility.epsilon);
        Vec3 localExhaustDirection = getParticleDebugExhaustDirectionLocal();
        Vec3 localNozzlePosition = getParticleDebugNozzlePositionLocal();

        // Convert local sublevel coordinates into world-space coordinates so particles align
        // with ship rotation instead of sticking to global axes.
        Vec3 worldNozzlePosition = Sable.HELPER.projectOutOfSubLevel(level, localNozzlePosition);
        Vec3 worldAheadPosition = Sable.HELPER.projectOutOfSubLevel(level, localNozzlePosition.add(localExhaustDirection));
        Vec3 worldExhaustDirection = worldAheadPosition.subtract(worldNozzlePosition);
        if (worldExhaustDirection.lengthSqr() < MathUtility.epsilon) {
            worldExhaustDirection = localExhaustDirection;
        } else {
            worldExhaustDirection = worldExhaustDirection.normalize();
        }

        Vector3d particleVelocity = new Vector3d(worldExhaustDirection.x, worldExhaustDirection.y, worldExhaustDirection.z)
                .mul(getParticleVelocity() * emissionScale * particleVelocityMultiplier);

        // Enough particles each tick so spacing along the velocity vector stays near TARGET_PARTICLE_SPACING_BLOCKS (no fractional carry → no skipped ticks).
        double speedPerTick = particleVelocity.length();
        double density = speedPerTick / TARGET_PARTICLE_SPACING_BLOCKS * particleCountMultiplier;
        int particlesToSpawn = Math.max(1, (int) Math.ceil(density));

        ParticleOptions particleData = createParticleOptions();

        double nozzleX = worldNozzlePosition.x;
        double nozzleY = worldNozzlePosition.y;
        double nozzleZ = worldNozzlePosition.z;

        for (int i = 0; i < particlesToSpawn; i++) {
            // Distribute particles along this tick's exhaust path: i=0 spawns at the nozzle,
            // later ones spawn progressively further down the plume to fill the visual gap
            // to the prior tick's particles (which have already moved by `particleVelocity`).
            double frac = (double) i / (double) particlesToSpawn;
            double spawnX = nozzleX + particleVelocity.x * frac;
            double spawnY = nozzleY + particleVelocity.y * frac;
            double spawnZ = nozzleZ + particleVelocity.z * frac;

            emitParticle(spawnX, spawnY, spawnZ, particleVelocity, particleData);
        }
    }

    protected void emitParticle(double spawnX, double spawnY, double spawnZ, Vector3d particleVelocity, ParticleOptions particleData) {
        if (level instanceof ServerLevel serverLevel) {
            double maxDistSq = getParticleBroadcastRange() * getParticleBroadcastRange();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(spawnX, spawnY, spawnZ) > maxDistSq) {
                    continue;
                }
                serverLevel.sendParticles(player, particleData, true, spawnX, spawnY, spawnZ, 0,
                        particleVelocity.x, particleVelocity.y, particleVelocity.z, 1.0);
            }
        } else {
            level.addParticle(particleData, true,
                    spawnX, spawnY, spawnZ, particleVelocity.x, particleVelocity.y, particleVelocity.z);
        }
    }

    @SuppressWarnings("deprecation") // i hate compilers let me use ts
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
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

        redstoneInput = compound.getInt("RedstoneInput");
        digitalInput = compound.getFloat("DigitalInput");
        if (compound.contains("ControlMode")) {
            controlMode = ControlMode.values()[compound.getInt("ControlMode")];
        }
        // Read thrust value from sync packets if present
        if (compound.contains("Thrust")) {
            thrusterData.setThrust(compound.getFloat("Thrust"));
        }
        dyeId = compound.contains("DyeId") ? compound.getString("DyeId") : null;
    }

}
