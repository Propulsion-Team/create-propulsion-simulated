package dev.propulsionteam.propulsionsimulated.content.thruster;

import net.createmod.catnip.animation.LerpedFloat;

/**
 * Client-side smoothed plume intensity/length, matching Create Aeronautics {@code GasEmitterRenderHandler}.
 */
public final class ThrusterPlumeVisuals {
    private final LerpedFloat position = LerpedFloat.linear();
    private final LerpedFloat fade = LerpedFloat.linear();

    public ThrusterPlumeVisuals() {
        position.chase(0.0, 0.2, LerpedFloat.Chaser.EXP);
        fade.chase(0.0, 0.2, LerpedFloat.Chaser.EXP);
    }

    public void clientTick(AbstractThrusterBlockEntity blockEntity) {
        float target = blockEntity.shouldRenderTexturePlume()
                ? blockEntity.getThrottle() * blockEntity.calculateObstructionEffect()
                : 0.0f;
        position.updateChaseTarget(target);
        position.tickChaser();

        float fadeTarget = position.getChaseTarget() > 0.0f || position.getValue() > 0.5f ? 1.0f : 0.0f;
        fade.updateChaseTarget(fadeTarget);
        fade.tickChaser();
    }

    public int getAlpha(float partialTick) {
        return (int) (fade.getValue(partialTick) * 255.0f);
    }

    public float getLength(float partialTick) {
        return position.getValue(partialTick);
    }
}
