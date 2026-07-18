package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlockEntity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * Ion FE thrusters use their own peripheral type so Lua can distinguish them from fuel thrusters ({@link ThrusterPeripheral}).
 */
public class IonThrusterPeripheral extends ThrusterPeripheralBase<IonThrusterBlockEntity> {

    public IonThrusterPeripheral(IonThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "ion_thruster";
    }

    @LuaFunction(mainThread = true)
    public final int getObstruction() {
        return blockEntity.getUnobstructedBlocks();
    }

    @LuaFunction(mainThread = true)
    public final void setPower(int redstonePower) {
        ThrusterComputerHelpers.setThrottleFromRedstone(blockEntity, redstonePower);
    }

    @LuaFunction(mainThread = true)
    public final void setPowerNormalized(double power) {
        ThrusterComputerHelpers.setThrottleNormalized(blockEntity, power);
    }

    @LuaFunction(mainThread = true)
    public final double getPower() {
        return blockEntity.getThrottle();
    }

    @LuaFunction(mainThread = true)
    public final double getCurrentThrustPN() {
        return blockEntity.getCurrentThrust();
    }

    @LuaFunction(mainThread = true)
    public final double getCurrentThrustKN() {
        return getCurrentThrustPN() / PropulsionConfig.getThrustUnitsPerKnOrDefault();
    }

    @LuaFunction(mainThread = true)
    public final double getDisplayedThrustPN() {
        return blockEntity.getDisplayedThrustPnForTooltip();
    }

    @LuaFunction(mainThread = true)
    public final double getDisplayedThrustKN() {
        return getDisplayedThrustPN() / PropulsionConfig.getThrustUnitsPerKnOrDefault();
    }

    @LuaFunction(mainThread = true)
    public final double getAirflowMs() {
        return blockEntity.getDisplayedAirflowMsForTooltip();
    }

    @LuaFunction(mainThread = true)
    public final int getEnergyAmountFe() {
        return blockEntity.getEnergyStoredFe();
    }

    @LuaFunction(mainThread = true)
    public final int getEnergyCapacityFe() {
        return blockEntity.getTotalEnergyCapacityForComputer();
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other)
            return true;
        if (other instanceof IonThrusterPeripheral ion)
            return this.blockEntity == ion.blockEntity;
        return false;
    }

}
