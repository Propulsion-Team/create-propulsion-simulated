package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import dev.propulsionteam.propulsionsimulated.content.hydropump.HydropumpBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.hydropump.HydropumpSpeedBehaviour;

public class HydropumpPeripheral extends SyncedPeripheral<HydropumpBlockEntity> {
    public HydropumpPeripheral(HydropumpBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "hydropump";
    }

    @LuaFunction(mainThread = true)
    public final int getPumpingSpeed() {
        return blockEntity.getPumpingSpeed();
    }

    @LuaFunction(mainThread = true)
    public final void setPumpingSpeed(int speed) {
        blockEntity.setPumpingSpeed(speed);
    }

    @LuaFunction
    public final int getMaxPumpingSpeed() {
        return HydropumpSpeedBehaviour.maxSpeed();
    }
}
