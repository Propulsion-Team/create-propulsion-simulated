package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

public class ThrusterPeripheral extends ThrusterPeripheralBase<ThrusterBlockEntity> {
    private final FluidMethods fluidMethods = new FluidMethods();

    public ThrusterPeripheral(ThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "thruster";
    }

    @LuaFunction
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

    @LuaFunction
    public final double getCurrentThrustPN() {
        return blockEntity.getCurrentThrust();
    }

    @LuaFunction
    public final double getCurrentThrustKN() {
        return getCurrentThrustPN() / PropulsionConfig.getThrustUnitsPerKnOrDefault();
    }

    @LuaFunction
    public final double getDisplayedThrustPN() {
        return blockEntity.getDisplayedThrustPnForTooltip();
    }

    @LuaFunction
    public final double getDisplayedThrustKN() {
        return getDisplayedThrustPN() / PropulsionConfig.getThrustUnitsPerKnOrDefault();
    }

    @LuaFunction
    public final double getAirflowMs() {
        return blockEntity.getDisplayedAirflowMsForTooltip();
    }

    @LuaFunction(mainThread = true)
    public final int getFuelAmountMb() {
        return blockEntity.getFuelAmountMb();
    }

    @LuaFunction(mainThread = true)
    public final int getFuelCapacityMb() {
        return blockEntity.getFuelCapacityMb();
    }

    //IFluidHandler methods passthrough
    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, ?>> tanks() throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.tanks(handler);
    }

    @LuaFunction(mainThread = true)
    public final int pushFluid(IComputerAccess computer, String toName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.pushFluid(handler, computer, toName, limit, fluidName);
    }

    @LuaFunction(mainThread = true)
    public final int pullFluid(IComputerAccess computer, String fromName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        IFluidHandler handler = getHandler();
        return this.fluidMethods.pullFluid(handler, computer, fromName, limit, fluidName);
    }

    private IFluidHandler getHandler() throws LuaException {
        IFluidHandler handler = blockEntity.getFluidHandler(blockEntity.getFacing());
        if (handler == null)
            throw new LuaException("Fluid tank not available");
        return handler;
    }

    //Boilerplate
    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof ThrusterPeripheral otherThruster) {
            return this.blockEntity == otherThruster.blockEntity;
        }
        return false;
    }

}
