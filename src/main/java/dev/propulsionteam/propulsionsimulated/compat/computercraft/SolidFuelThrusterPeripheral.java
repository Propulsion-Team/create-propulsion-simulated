package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.generic.methods.InventoryMethods;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.thruster.AbstractThrusterBlockEntity.ControlMode;
import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Solid item-fuel thrusters use their own peripheral type so Lua can distinguish them from fluid thrusters.
 */
public class SolidFuelThrusterPeripheral extends ThrusterPeripheralBase<SolidFuelThrusterBlockEntity> {
    private final InventoryMethods inventoryMethods = new InventoryMethods();

    public SolidFuelThrusterPeripheral(SolidFuelThrusterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "solid_fuel_thruster";
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
        return ThrusterComputerHelpers.toDisplayed(blockEntity.getCurrentThrust());
    }

    @LuaFunction
    public final double getAirflowMs() {
        return blockEntity.getDisplayedAirflowMsForTooltip();
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getFuelItem() {
        ItemStack fuel = blockEntity.getFuelStack();
        if (fuel.isEmpty()) return null;
        return Map.of("name", BuiltInRegistries.ITEM.getKey(fuel.getItem()).toString(), "count", fuel.getCount());
    }

    @LuaFunction(mainThread = true)
    public final int getBurnTimeRemaining() {
        return blockEntity.getBurnTime();
    }

    @LuaFunction(mainThread = true)
    public final boolean isBurning() {
        return blockEntity.getBurnTime() > 0 && !blockEntity.getBurningFuel().isEmpty()
            && blockEntity.getThrottle() > 0;
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, Map<String, ?>> list() throws LuaException {
        return inventoryMethods.list(getHandler());
    }

    @LuaFunction(mainThread = true)
    public final int pushItems(IComputerAccess computer, String toName, int fromSlot, Optional<Integer> limit, Optional<Integer> toSlot)
        throws LuaException {
        return inventoryMethods.pushItems(getHandler(), computer, toName, fromSlot, limit, toSlot);
    }

    @LuaFunction(mainThread = true)
    public final int pullItems(IComputerAccess computer, String fromName, int toSlot, Optional<Integer> limit, Optional<Integer> fromSlot)
        throws LuaException {
        return inventoryMethods.pullItems(getHandler(), computer, fromName, toSlot, limit, fromSlot);
    }

    private IItemHandler getHandler() throws LuaException {
        IItemHandler handler = blockEntity.inventory;
        if (handler == null) {
            throw new LuaException("Item inventory not available");
        }
        return handler;
    }

    @Override
    public boolean equals(IPeripheral other) {
        if (this == other) {
            return true;
        }
        if (other instanceof SolidFuelThrusterPeripheral otherThruster) {
            return this.blockEntity == otherThruster.blockEntity;
        }
        return false;
    }

}
