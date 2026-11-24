package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.FluidMethods;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.rcs_thruster.RcsThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterThrottleMath;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RcsThrusterPeripheral implements IPeripheral {
    private final RcsThrusterBlockEntity blockEntity;
    private final FluidMethods fluidMethods = new FluidMethods();
    private final Set<IComputerAccess> attachedComputers = Collections.newSetFromMap(new IdentityHashMap<>());

    public RcsThrusterPeripheral(RcsThrusterBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public String getType() {
        return "rcs_thruster";
    }

    @LuaFunction(mainThread = true)
    public final int getNozzleCount() {
        return blockEntity.links.length;
    }

    @LuaFunction(mainThread = true)
    public final void setNozzlePower(int nozzle, int power) throws LuaException {
        blockEntity.setNozzleOverride(index(nozzle), Math.max(0, Math.min(15, power)) / 15.0f);
    }

    @LuaFunction(mainThread = true)
    public final void setNozzlePowerNormalized(int nozzle, double power) throws LuaException {
        blockEntity.setNozzleOverride(index(nozzle), ThrusterThrottleMath.clampNormalized(power));
    }

    @LuaFunction(mainThread = true)
    public final void setNozzleThrustPN(int nozzle, double thrustPn) throws LuaException {
        double max = blockEntity.thrust();
        blockEntity.setNozzleOverride(index(nozzle), ThrusterThrottleMath.clampNormalized(max > 0 ? thrustPn / max : 0));
    }

    @LuaFunction(mainThread = true)
    public final double getNozzlePower(int nozzle) throws LuaException {
        return blockEntity.getNozzlePower(index(nozzle));
    }

    @LuaFunction(mainThread = true)
    public final double getNozzleThrustPN(int nozzle) throws LuaException {
        return blockEntity.nozzleThrust(index(nozzle));
    }

    @LuaFunction(mainThread = true)
    public final double getCurrentThrustPN() {
        return blockEntity.currentThrust();
    }

    @LuaFunction(mainThread = true)
    public final double getCurrentThrustKN() {
        return blockEntity.currentThrust() / PropulsionConfig.getThrustUnitsPerKnOrDefault();
    }

    @LuaFunction(mainThread = true)
    public final double getMaxThrustPN() {
        return blockEntity.thrust();
    }

    @LuaFunction(mainThread = true)
    public final double getMaxThrustKN() {
        return blockEntity.thrust() / PropulsionConfig.getThrustUnitsPerKnOrDefault();
    }

    @LuaFunction(mainThread = true)
    public final boolean isBoosted() {
        return blockEntity.isBoosted();
    }

    @LuaFunction(mainThread = true)
    public final int getFuelAmountMb() {
        return blockEntity.tank.getPrimaryHandler().getFluidAmount();
    }

    @LuaFunction(mainThread = true)
    public final int getFuelCapacityMb() {
        return blockEntity.tank.getPrimaryHandler().getCapacity();
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, ?>> tanks() throws LuaException {
        return fluidMethods.tanks(handler());
    }

    @LuaFunction(mainThread = true)
    public final int pushFluid(IComputerAccess computer, String toName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        return fluidMethods.pushFluid(handler(), computer, toName, limit, fluidName);
    }

    @LuaFunction(mainThread = true)
    public final int pullFluid(IComputerAccess computer, String fromName, Optional<Integer> limit, Optional<String> fluidName) throws LuaException {
        return fluidMethods.pullFluid(handler(), computer, fromName, limit, fluidName);
    }

    private IFluidHandler handler() {
        return blockEntity.tank.getPrimaryHandler();
    }

    private int index(int nozzle) throws LuaException {
        if (nozzle < 1 || nozzle > blockEntity.links.length) {
            throw new LuaException("Nozzle must be between 1 and " + blockEntity.links.length);
        }
        return nozzle - 1;
    }

    @Override
    public void attach(@NotNull IComputerAccess computer) {
        synchronized (attachedComputers) {
            attachedComputers.add(computer);
        }
    }

    @Override
    public void detach(@NotNull IComputerAccess computer) {
        boolean last;
        synchronized (attachedComputers) {
            attachedComputers.remove(computer);
            last = attachedComputers.isEmpty();
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (last && server != null) {
            server.execute(blockEntity::clearOverrides);
        }
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof RcsThrusterPeripheral peripheral && blockEntity == peripheral.blockEntity;
    }
}
