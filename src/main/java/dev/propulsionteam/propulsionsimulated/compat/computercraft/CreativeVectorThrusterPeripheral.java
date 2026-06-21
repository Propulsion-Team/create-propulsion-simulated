package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster.CreativeVectorThrusterBlockEntity;

/**
 * Creative vector thrusters use their own peripheral class so Lua lists only relevant methods and {@link #getType()} is distinct.
 */
public class CreativeVectorThrusterPeripheral extends VectorThrusterPeripheral {

    public CreativeVectorThrusterPeripheral(CreativeVectorThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "creative_vector_thruster";
    }

    @LuaFunction(mainThread = true)
    public final void setThrustOutput(double thrustOutputPn) {
        creative().setThrustOutput((float) thrustOutputPn);
    }

    /** Clears CC thrust override; base thrust follows the on-block scroll again. Same as {@code setThrustOutput(-1)}. */
    @LuaFunction(mainThread = true)
    public final void clearThrustOutput() {
        creative().clearPeripheralThrustOutput();
    }

    /** Maximum base thrust in pN allowed by config (same cap as the scroll). */
    @LuaFunction
    public final double getMaxThrustOutputPn() {
        return PropulsionConfig.CREATIVE_VECTOR_THRUSTER_MAX_THRUST.get() * PropulsionConfig.getThrustUnitsPerKnOrDefault();
    }

    @LuaFunction
    public final boolean isCustomThrustOutputActive() {
        return creative().hasPeripheralThrustOverride();
    }

    private CreativeVectorThrusterBlockEntity creative() {
        return (CreativeVectorThrusterBlockEntity) blockEntity;
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other)
            return true;
        if (other instanceof CreativeVectorThrusterPeripheral p)
            return this.blockEntity == p.blockEntity;
        return false;
    }
}
