package dev.propulsionteam.propulsionsimulated.content.tilt_adapter;

/**
 * Server-authoritative angle state for a tilt adapter.
 *
 * <p>The requested target may change at any time, while an active segment keeps
 * its captured endpoint until it has physically completed.</p>
 */
final class TiltAdapterMotionState {
    static final float EPSILON = 0.001f;

    private float requestedTarget;
    private float activeTarget;
    private float currentAngle;
    private float remainingAngle;
    private int activeDirection;

    float requestedTarget() {
        return requestedTarget;
    }

    float activeTarget() {
        return activeTarget;
    }

    float currentAngle() {
        return currentAngle;
    }

    float remainingAngle() {
        return remainingAngle;
    }

    int activeDirection() {
        return activeDirection;
    }

    float renderTarget() {
        return isActive() ? activeTarget : currentAngle;
    }

    boolean isActive() {
        return activeDirection != 0;
    }

    boolean needsSegment() {
        return !isActive() && Math.abs(requestedTarget - currentAngle) > EPSILON;
    }

    void requestTarget(float target) {
        requestedTarget = finiteOr(target, currentAngle);
    }

    boolean startSegment() {
        return startSegment(Float.POSITIVE_INFINITY);
    }

    boolean startSegment(float maximumSegmentAngle) {
        if (!needsSegment()) {
            return false;
        }

        float delta = requestedTarget - currentAngle;
        float segmentAngle = Float.isFinite(maximumSegmentAngle) && maximumSegmentAngle > EPSILON
            ? Math.min(Math.abs(delta), maximumSegmentAngle)
            : Math.abs(delta);
        activeTarget = currentAngle + Math.copySign(segmentAngle, delta);
        delta = activeTarget - currentAngle;
        activeDirection = (int) Math.signum(delta);
        remainingAngle = Math.abs(delta);
        return true;
    }

    /**
     * Advances the active segment without clearing it. The caller must detach
     * the propagated output before calling {@link #finishSegment()}.
     */
    boolean advance(float maximumStep) {
        if (!isActive() || maximumStep <= 0 || !Float.isFinite(maximumStep)) {
            return false;
        }

        float actualStep = Math.min(maximumStep, remainingAngle);
        currentAngle += actualStep * activeDirection;
        remainingAngle -= actualStep;

        if (remainingAngle <= EPSILON) {
            currentAngle = activeTarget;
            remainingAngle = 0;
            return true;
        }
        return false;
    }

    void finishSegment() {
        activeDirection = 0;
        remainingAngle = 0;
        activeTarget = currentAngle;
    }

    void cancelSegment() {
        finishSegment();
    }

    void restore(float requestedTarget, float activeTarget, float currentAngle,
        float remainingAngle, int activeDirection) {
        this.currentAngle = finiteOr(currentAngle, 0);
        this.requestedTarget = finiteOr(requestedTarget, this.currentAngle);
        this.activeTarget = finiteOr(activeTarget, this.currentAngle);

        float activeDelta = this.activeTarget - this.currentAngle;
        int restoredDirection = (int) Math.signum(activeDelta);
        if (activeDirection == restoredDirection && activeDirection != 0
            && finiteOr(remainingAngle, 0) > EPSILON) {
            this.activeDirection = restoredDirection;
            // The endpoint is authoritative; old saves could contain stale remaining values.
            this.remainingAngle = Math.abs(activeDelta);
        } else {
            finishSegment();
        }
    }

    void offsetAngles(float offset) {
        requestedTarget += offset;
        activeTarget += offset;
        currentAngle += offset;
    }

    void clampTargets(float minimum, float maximum) {
        requestedTarget = clamp(requestedTarget, minimum, maximum);
        if (isActive()) {
            activeTarget = clamp(activeTarget, minimum, maximum);
            float delta = activeTarget - currentAngle;
            if (Math.abs(delta) <= EPSILON) {
                finishSegment();
            } else {
                activeDirection = (int) Math.signum(delta);
                remainingAngle = Math.abs(delta);
            }
        }
    }

    static float computeRedstoneTarget(int leftSignal, int rightSignal, float neutral,
        float positiveRange, float negativeRange) {
        float fromLeft = (leftSignal / 15.0f) * positiveRange;
        float fromRight = (rightSignal / 15.0f) * negativeRange;
        return neutral + fromLeft - fromRight;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
