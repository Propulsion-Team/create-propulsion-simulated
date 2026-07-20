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
    private static final int SEGMENT_SETTLE_TICKS = 2;
    // Avoid Create's ambiguous shortest-angle interpolation at exactly 180 degrees.
    private static final float MAX_PROPAGATED_SEGMENT_ANGLE = 179.0f;

    protected int redstoneLeft = 0;
    protected int redstoneRight = 0;
    
    protected final TiltAdapterMotionState motion = new TiltAdapterMotionState();
    protected float computerTargetAngle = 0f;
    private boolean segmentStartQueued;
    private int segmentSettleTicks;

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
        if (motion.isActive()) {
            if (speed <= 0) {
                cancelActiveSegment();
            } else if (segmentSettleTicks > 0) {
                segmentSettleTicks--;
                if (segmentSettleTicks == 0) {
                    finishActiveSegment();
                }
            } else if (motion.advance(KineticBlockEntity.convertToAngular(speed))) {
                // Create's sequenced gearshift deliberately allows two extra ticks
                // after the calculated duration. This lets downstream consumers
                // clamp to the propagated endpoint regardless of BE tick order.
                segmentSettleTicks = SEGMENT_SETTLE_TICKS;
                sendData();
            } else {
                sendData();
            }
        }

        checkRedstoneTarget();
        queueSegmentStartIfNeeded();
    }

    public int getLeft() { return redstoneLeft; }
    public int getRight() { return redstoneRight; }
    public float getCurrentAngle() { return motion.currentAngle(); }
    public float getTargetAngle() { return motion.requestedTarget(); }
    public float getRenderTargetAngle() { return motion.renderTarget(); }

    public void setComputerTargetAngle(float angle) {
        computerTargetAngle = angle;
    }

    protected void checkRedstoneTarget() {
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

        if (Math.abs(newTarget - motion.requestedTarget()) > TiltAdapterMotionState.EPSILON) {
            motion.requestTarget(newTarget);
            sendData();
        }
    }

    protected void requestTargetRecalculation() {
        motion.requestTarget(clampToAngleLimits(computeTargetAngle()));
        queueSegmentStartIfNeeded();
        sendData();
    }

    private void queueSegmentStartIfNeeded() {
        if (segmentStartQueued || motion.isActive() || !motion.needsSegment()
            || Math.abs(getTheoreticalSpeed()) <= 0) {
            return;
        }

        segmentStartQueued = true;
        flickerTicker.scheduleUpdate(() -> {
            segmentStartQueued = false;
            startPendingSegment();
        });
    }

    /** Starts one immutable kinetic segment toward the newest requested target. */
    protected void startPendingSegment() {
        float speed = Math.abs(getTheoreticalSpeed());
        if (speed <= 0 || motion.isActive() || !motion.startSegment(MAX_PROPAGATED_SEGMENT_ANGLE)) {
            return;
        }

        sequenceContext = new SequenceContext(
            SequencerInstructions.TURN_ANGLE,
            motion.remainingAngle() / speed
        );

        attachKinetics();
        sendData();
    }

    private void finishActiveSegment() {
        // Detach while the old modifier is still visible so the complete output
        // subtree is stopped before the active segment is cleared.
        detachKinetics();
        motion.finishSegment();
        segmentSettleTicks = 0;
        sequenceContext = null;
        sendData();
    }

    private void cancelActiveSegment() {
        detachKinetics();
        motion.cancelSegment();
        segmentSettleTicks = 0;
        sequenceContext = null;
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
        return TiltAdapterMotionState.computeRedstoneTarget(
            redstoneLeft,
            redstoneRight,
            getNeutralTargetAngle(),
            getPositiveSideAngleRange(),
            getNegativeSideAngleRange()
        );
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

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        Direction directionToTarget = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
        if (directionToTarget == getBackFace(stateFrom)) return 0;
        return super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (face == AbstractTiltAdapterBlock.getDirection(getBlockState())) return motion.activeDirection();
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
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putFloat("targetAngle", motion.requestedTarget());
        compound.putFloat("networkTargetAngle", motion.activeTarget());
        compound.putFloat("currentAngle", motion.currentAngle());
        compound.putInt("activeMoveDirection", motion.activeDirection());
        compound.putFloat("activeSequenceLimit", motion.remainingAngle());
        compound.putInt("redstoneLeft", redstoneLeft);
        compound.putInt("redstoneRight", redstoneRight);
        compound.putFloat("computerTargetAngle", computerTargetAngle);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        motion.restore(
            compound.getFloat("targetAngle"),
            compound.getFloat("networkTargetAngle"),
            compound.getFloat("currentAngle"),
            compound.getFloat("activeSequenceLimit"),
            compound.getInt("activeMoveDirection")
        );
        redstoneLeft = compound.getInt("redstoneLeft");
        redstoneRight = compound.getInt("redstoneRight");
        computerTargetAngle = compound.getFloat("computerTargetAngle");
    }

    @Override protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {}
    @Override protected boolean syncSequenceContext() { return true; }
}
