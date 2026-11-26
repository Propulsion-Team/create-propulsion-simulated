package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterThrottleMath;
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

    static double toDisplayed(double thrust) {
        return thrust / PropulsionConfig.getThrustUnitsPerPnOrDefault();
    }

    static void setThrustPn(AbstractThrusterBlockEntity be, double thrustPn) {
        double max = be.getMaxThrustPn();
        setThrottleNormalized(be, max > 0.0d ? thrustPn / max : 0.0d);
    }

    static void returnToRedstone(AbstractThrusterBlockEntity be) {
        be.setDigitalInput(0.0f);
        if (be.getLevel() != null) {
            be.setRedstoneInput(be.getLevel().getBestNeighborSignal(be.getBlockPos()));
        }
        be.setControlMode(AbstractThrusterBlockEntity.ControlMode.NORMAL);
    }

    static void setThrottleNormalized(AbstractThrusterBlockEntity be, double normalized) {
        be.setControlMode(AbstractThrusterBlockEntity.ControlMode.PERIPHERAL);
        be.setDigitalInput(ThrusterThrottleMath.clampNormalized(normalized));
    }
}
