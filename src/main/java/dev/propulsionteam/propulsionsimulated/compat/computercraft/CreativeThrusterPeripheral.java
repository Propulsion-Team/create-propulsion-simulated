package dev.propulsionteam.propulsionsimulated.compat.computercraft;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlockEntity;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class CreativeThrusterPeripheral extends ThrusterPeripheralBase<CreativeThrusterBlockEntity> {

    public CreativeThrusterPeripheral(CreativeThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "creative_thruster";
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

    @LuaFunction(mainThread = true)
    public final void setThrustConfig(int percent) {
        blockEntity.setThrustConfig(percent);
    }

    @LuaFunction
    public final int getThrustConfig() {
        return blockEntity.getThrustConfig();
    }

    @LuaFunction
    public final float getTargetThrustPN() {
        return blockEntity.getCreativeTargetThrust();
    }

    @LuaFunction
    public final float getTargetThrustKN() {
        return (float) (getTargetThrustPN() / PropulsionConfig.getThrustUnitsPerKnOrDefault());
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

    //Boilerplate
    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) return true;
        if (other instanceof CreativeThrusterPeripheral otherThruster) {
            return this.blockEntity == otherThruster.blockEntity;
        }
        return false;
    }

}
