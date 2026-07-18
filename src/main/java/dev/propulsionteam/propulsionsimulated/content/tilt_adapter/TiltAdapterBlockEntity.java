package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import java.util.List;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.compat.PropulsionCompatibility;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerBehaviour;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.utility.FlickerAwareTicker;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

public class TiltAdapterBlockEntity extends SplitShaftBlockEntity {
    public static final float SIGNAL_RANGE = 15.0f;

    protected int redstoneLeft = 0;
    protected int redstoneRight = 0;
    
    protected float targetAngle = 0f;
    protected float networkTargetAngle = 0f;
    protected float currentAngle = 0f;
    protected int activeMoveDirection = 0;
    protected float activeSequenceLimit = 0f;
    protected float computerTargetAngle = 0f;

    public FlickerAwareTicker flickerTicker;
    public AbstractComputerBehaviour computerBehaviour;

    public TiltAdapterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TiltAdapterBlockEntity(BlockPos pos, BlockState state) {
        this(PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        flickerTicker = new FlickerAwareTicker(this, 60); 
        behaviours.add(flickerTicker);

        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        float speed = Math.abs(getTheoreticalSpeed());
        if (activeMoveDirection != 0 && speed > 0 && activeSequenceLimit > 0) {
            float step = KineticBlockEntity.convertToAngular(speed);
            float actualStep = Math.min(step, activeSequenceLimit);

            currentAngle += actualStep * activeMoveDirection;
            activeSequenceLimit -= actualStep;

            if (activeSequenceLimit <= 0) {
                activeSequenceLimit = 0;
                // The propagated network completed the target captured when this
                // sequence began. A newer command may still be waiting in the
                // flicker-aware scheduler and has not physically moved yet.
                currentAngle = clampToAngleLimits(networkTargetAngle);
                flickerTicker.scheduleUpdate(this::syncNetworkState);
            }
            sendData();
        }

        checkRedstoneAndSpeed();
    }

    public int getLeft() { return redstoneLeft; }
    public int getRight() { return redstoneRight; }
    public float getCurrentAngle() { return currentAngle; }
    public float getTargetAngle() { return targetAngle; }

    public void setComputerTargetAngle(float angle) {
        computerTargetAngle = angle;
    }

    protected void checkRedstoneAndSpeed() {
        Level level = getLevel();
        if (level == null) return;

        BlockState state = getBlockState();
        Direction posSignalSide = AbstractTiltAdapterBlock.getRedstoneSide(state, true);
        Direction negSignalSide = AbstractTiltAdapterBlock.getRedstoneSide(state, false);

        int newLeft = level.getSignal(worldPosition.relative(posSignalSide), posSignalSide);
        int newRight = level.getSignal(worldPosition.relative(negSignalSide), negSignalSide);
        
        if (newLeft != redstoneLeft || newRight != redstoneRight) {
            redstoneLeft = newLeft;
            redstoneRight = newRight;
            sendData();
        }

        float newTarget = clampToAngleLimits(computeTargetAngle());

        if (Math.abs(newTarget - targetAngle) > 0.001f) {
            targetAngle = newTarget;
            // Coalesce rapid analog/CC changes instead of repeatedly detaching the
            // same kinetic network in one burst.
            flickerTicker.scheduleUpdate(this::syncNetworkState);
        }
    }

    /** Starts or extends a kinetic move toward {@link #targetAngle} (call when speed &gt; 0). */
    protected void beginOrExtendKineticMove() {
        float speed = Math.abs(getTheoreticalSpeed());
        float delta = targetAngle - currentAngle;
        if (speed <= 0 || Math.abs(delta) <= 0.001f) {
            return;
        }

        networkTargetAngle = targetAngle;
        activeMoveDirection = (int) Math.signum(delta);
        activeSequenceLimit = Math.abs(delta);

        float kineticSpeed = speed * activeMoveDirection;
        sequenceContext = new SequenceContext(
            SequencerInstructions.TURN_ANGLE,
            activeSequenceLimit / Math.abs(kineticSpeed)
        );

        detachKinetics();
        attachKinetics();
        sendData();
    }

    protected float getNeutralTargetAngle() {
        return 0f;
    }

    protected float computeTargetAngle() {
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour != null && computerBehaviour.hasAttachedComputer()) {
            return clampToAngleLimits(computerTargetAngle);
        }

        if (redstoneLeft == 0 && redstoneRight == 0) {
            return getNeutralTargetAngle();
        }

        // Each redstone face scales to its own limit (matches left/right value boxes).
        float fromLeft = (redstoneLeft / SIGNAL_RANGE) * getPositiveSideAngleRange();
        float fromRight = (redstoneRight / SIGNAL_RANGE) * getNegativeSideAngleRange();
        return getNeutralTargetAngle() + fromLeft - fromRight;
    }

    protected float clampToAngleLimits(float angle) {
        float neutral = getNeutralTargetAngle();
        return Mth.clamp(angle, neutral - getNegativeSideAngleRange(), neutral + getPositiveSideAngleRange());
    }

    protected float getPositiveSideAngleRange() {
        return PropulsionConfig.TILT_ADAPTER_ANGLE_RANGE.get().floatValue();
    }

    protected float getNegativeSideAngleRange() {
        return getPositiveSideAngleRange();
    }

    protected void syncNetworkState() {
        float speed = Math.abs(getTheoreticalSpeed());
        float delta = targetAngle - currentAngle;

        if (Math.abs(delta) > 0.001f && speed > 0) {
            beginOrExtendKineticMove();
        } else {
            activeMoveDirection = 0;
            activeSequenceLimit = 0;
            sequenceContext = null;
            currentAngle = clampToAngleLimits(targetAngle);
            detachKinetics();
            attachKinetics();
            sendData();
        }
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        Direction directionToTarget = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
        if (directionToTarget == getBackFace(stateFrom)) return 0;
        return super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (face == AbstractTiltAdapterBlock.getDirection(getBlockState())) return activeMoveDirection;
        if (hasSource() && getSourceFacing() != getBackFace(getBlockState())) return 0;
        return 1;
    }

    protected Direction getBackFace(BlockState state) {
        if (state.hasProperty(DirectionalKineticBlock.FACING)) {
            return state.getValue(DirectionalKineticBlock.FACING).getOpposite();
        }
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        boolean positive = state.hasProperty(AbstractTiltAdapterBlock.POSITIVE) ? state.getValue(AbstractTiltAdapterBlock.POSITIVE) : true;
        return Direction.get(positive ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE, axis);
    }

    @Override
    public void initialize() {
        super.initialize();
        Level level = getLevel();
        if (level != null && !level.isClientSide) flickerTicker.scheduleUpdate(this::syncNetworkState);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putFloat("targetAngle", targetAngle);
        compound.putFloat("networkTargetAngle", networkTargetAngle);
        compound.putFloat("currentAngle", currentAngle);
        compound.putInt("activeMoveDirection", activeMoveDirection);
        compound.putFloat("activeSequenceLimit", activeSequenceLimit);
        compound.putInt("redstoneLeft", redstoneLeft);
        compound.putInt("redstoneRight", redstoneRight);
        compound.putFloat("computerTargetAngle", computerTargetAngle);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        targetAngle = compound.getFloat("targetAngle");
        networkTargetAngle = compound.getFloat("networkTargetAngle");
        currentAngle = compound.getFloat("currentAngle");
        activeMoveDirection = compound.getInt("activeMoveDirection");
        activeSequenceLimit = compound.getFloat("activeSequenceLimit");
        redstoneLeft = compound.getInt("redstoneLeft");
        redstoneRight = compound.getInt("redstoneRight");
        computerTargetAngle = compound.getFloat("computerTargetAngle");
    }

    @Override protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {}
    @Override protected boolean syncSequenceContext() { return true; }
}
