package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TiltAdapterMotionStateTest {
    private static final float EPSILON = 1.0e-5f;

    @Test
    void completesExactEndpointWithoutOvershoot() {
        TiltAdapterMotionState motion = new TiltAdapterMotionState();
        motion.requestTarget(90);
        assertTrue(motion.startSegment());

        assertFalse(motion.advance(40));
        assertFalse(motion.advance(40));
        assertTrue(motion.advance(40));
        assertEquals(90, motion.currentAngle(), EPSILON);
        assertEquals(0, motion.remainingAngle(), EPSILON);

        motion.finishSegment();
        assertFalse(motion.isActive());
    }

    @Test
    void activeEndpointIsImmutableWhileLatestRequestIsCoalesced() {
        TiltAdapterMotionState motion = new TiltAdapterMotionState();
        motion.requestTarget(90);
        assertTrue(motion.startSegment());
        assertFalse(motion.advance(30));

        motion.requestTarget(-45);
        assertEquals(90, motion.activeTarget(), EPSILON);
        assertEquals(-45, motion.requestedTarget(), EPSILON);

        assertTrue(motion.advance(100));
        motion.finishSegment();
        assertTrue(motion.startSegment());
        assertEquals(-1, motion.activeDirection());
        assertEquals(135, motion.remainingAngle(), EPSILON);
    }

    @Test
    void oneTickPulseCompletesThenReturnsToLatestTarget() {
        TiltAdapterMotionState motion = new TiltAdapterMotionState();
        motion.requestTarget(90);
        assertTrue(motion.startSegment());
        motion.requestTarget(0);

        assertTrue(motion.advance(90));
        motion.finishSegment();
        assertTrue(motion.startSegment());
        assertTrue(motion.advance(90));
        motion.finishSegment();

        assertEquals(0, motion.currentAngle(), EPSILON);
        assertFalse(motion.needsSegment());
    }

    @Test
    void rapidAlternationKeepsOnlyNewestPendingTarget() {
        TiltAdapterMotionState motion = new TiltAdapterMotionState();
        motion.requestTarget(90);
        assertTrue(motion.startSegment());

        for (int i = 0; i < 100; i++) {
            motion.requestTarget(i % 2 == 0 ? -90 : 45);
        }

        assertEquals(90, motion.activeTarget(), EPSILON);
        assertEquals(45, motion.requestedTarget(), EPSILON);
        assertTrue(motion.advance(90));
        motion.finishSegment();
        assertTrue(motion.startSegment());
        assertEquals(45, motion.activeTarget(), EPSILON);
    }

    @Test
    void cancellingAtZeroSpeedResumesFromActualAngle() {
        TiltAdapterMotionState motion = new TiltAdapterMotionState();
        motion.requestTarget(90);
        assertTrue(motion.startSegment());
        assertFalse(motion.advance(35));

        motion.cancelSegment();
        assertEquals(35, motion.currentAngle(), EPSILON);
        assertTrue(motion.startSegment());
        assertEquals(55, motion.remainingAngle(), EPSILON);
    }

    @Test
    void restoreRecomputesStaleRemainingDistance() {
        TiltAdapterMotionState motion = new TiltAdapterMotionState();
        motion.restore(90, 90, 30, 999, 1);
        assertTrue(motion.isActive());
        assertEquals(60, motion.remainingAngle(), EPSILON);
    }

    @Test
    void redstoneTargetsSupportRegularAndAsymmetricRanges() {
        assertEquals(90, TiltAdapterMotionState.computeRedstoneTarget(15, 0, 0, 90, 90), EPSILON);
        assertEquals(-90, TiltAdapterMotionState.computeRedstoneTarget(0, 15, 0, 90, 90), EPSILON);
        assertEquals(30, TiltAdapterMotionState.computeRedstoneTarget(15, 5, 0, 60, 90), EPSILON);
        assertEquals(180, TiltAdapterMotionState.computeRedstoneTarget(15, 0, 0, 180, 45), EPSILON);
    }

    @Test
    void oldAdvancedCoordinatesRebaseWithoutChangingDelta() {
        TiltAdapterMotionState motion = new TiltAdapterMotionState();
        motion.restore(0, 0, -20, 20, 1);
        motion.offsetAngles(45);

        assertEquals(45, motion.requestedTarget(), EPSILON);
        assertEquals(45, motion.activeTarget(), EPSILON);
        assertEquals(25, motion.currentAngle(), EPSILON);
        assertEquals(20, motion.remainingAngle(), EPSILON);
    }

    @Test
    void exactHalfTurnIsSplitButStillReachesOneHundredEightyDegrees() {
        TiltAdapterMotionState motion = new TiltAdapterMotionState();
        motion.requestTarget(180);

        assertTrue(motion.startSegment(179));
        assertEquals(179, motion.activeTarget(), EPSILON);
        assertTrue(motion.advance(179));
        motion.finishSegment();

        assertTrue(motion.startSegment(179));
        assertEquals(180, motion.activeTarget(), EPSILON);
        assertEquals(1, motion.remainingAngle(), EPSILON);
        assertTrue(motion.advance(1));
        motion.finishSegment();

        assertEquals(180, motion.currentAngle(), EPSILON);
        assertFalse(motion.needsSegment());
    }

}
