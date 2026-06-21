package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster.LiquidVectorThrusterBlockEntity;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class LiquidVectorThrusterPeripheral extends SyncedPeripheral<LiquidVectorThrusterBlockEntity> {
    private final FluidMethods fluidMethods = new FluidMethods();

    public LiquidVectorThrusterPeripheral(LiquidVectorThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "liquid_vector_thruster";
    }

    @LuaFunction
    public final double getVectorX() {
        return blockEntity.getCurrentVectorX();
    }

    @LuaFunction
    public final double getVectorY() {
        return blockEntity.getCurrentVectorY();
    }

    @LuaFunction
    public final double getTargetVectorX() {
        return blockEntity.getTargetVectorX();
    }

    @LuaFunction
    public final double getTargetVectorY() {
        return blockEntity.getTargetVectorY();
    }

    @LuaFunction(mainThread = true)
    public final void setVectorX(double x) {
        blockEntity.setVectorCoordinates((float) Mth.clamp(x, -1.0, 1.0), blockEntity.getTargetVectorY());
    }

    @LuaFunction(mainThread = true)
    public final void setVectorY(double y) {
        blockEntity.setVectorCoordinates(blockEntity.getTargetVectorX(), (float) Mth.clamp(y, -1.0, 1.0));
    }

    @LuaFunction(mainThread = true)
    public final void setVector(double x, double y) {
        blockEntity.setVectorCoordinates((float) Mth.clamp(x, -1.0, 1.0), (float) Mth.clamp(y, -1.0, 1.0));
    }

    @LuaFunction(mainThread = true)
    public final void setThrust(int power) {
        ThrusterComputerHelpers.setThrottleFromRedstone(blockEntity, Mth.clamp(power, 0, 15));
    }

    @LuaFunction(mainThread = true)
    public final void setThrustNormalized(double power) {
        ThrusterComputerHelpers.setThrottleNormalized(blockEntity, power);
    }

    @LuaFunction(mainThread = true)
    public final void setPower(int power) {
        setThrust(power);
    }

    @LuaFunction(mainThread = true)
    public final void setPowerNormalized(double power) {
        setThrustNormalized(power);
    }

    @LuaFunction
    public final int getThrust() {
        return blockEntity.getLegacyPowerInt();
    }

    @LuaFunction
    public final double getPower() {
        return blockEntity.getPower();
    }

    // IFluidHandler methods passthrough
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

    @Override
    public void attach(@NotNull IComputerAccess computer) {
        super.attach(computer);
        blockEntity.setDigitalInput(Mth.clamp(blockEntity.getPower(), 0.0f, 1.0f));
        blockEntity.setControlMode(ControlMode.PERIPHERAL);
    }

    @Override
    public void detach(@NotNull IComputerAccess computer) {
        super.detach(computer);
        blockEntity.setDigitalInput(0.0f);
        blockEntity.setRedstonePower(0);
        blockEntity.setControlMode(ControlMode.NORMAL);
    }
}
