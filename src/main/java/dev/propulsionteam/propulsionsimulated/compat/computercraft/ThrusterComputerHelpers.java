package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import net.minecraft.util.Mth;

/**
 * ComputerCraft peripherals control throttle via {@link AbstractThrusterBlockEntity#setDigitalInput(float)}
 * while {@code ControlMode.PERIPHERAL} is active — not {@link AbstractThrusterBlockEntity#setRedstonePower(int)},
 * which does not refresh thrust in peripheral mode.
 */
final class ThrusterComputerHelpers {
    private ThrusterComputerHelpers() {}

    static void setThrottleFromRedstone(AbstractThrusterBlockEntity be, int redstonePower) {
        int clamped = Mth.clamp(redstonePower, 0, 15);
        be.setControlMode(AbstractThrusterBlockEntity.ControlMode.PERIPHERAL);
        be.setDigitalInput(clamped / 15.0f);
    }

    static void setThrottleNormalized(AbstractThrusterBlockEntity be, double normalized) {
        int redstonePower = Mth.floor(Mth.clamp(normalized, 0.0d, 1.0d) * 15.0d + 1.0e-6d);
        setThrottleFromRedstone(be, redstonePower);
    }
}
