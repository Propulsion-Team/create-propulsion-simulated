package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/** Fuel vector thrusters only; creative vectors use {@link CreativeVectorThrusterPeripheral}. */
public class VectorThrusterPeripheral extends ThrusterPeripheralBase<VectorThrusterBlockEntity> {
    public VectorThrusterPeripheral(VectorThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "vector_thruster";
    }

    // --- Vector control (float -1..1 coordinates) --------------------------

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

    // --- Throttle ----------------------------------------------------------

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

}
