package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import java.util.function.Function;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.FallbackComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;

public class CCProxy {
    public static void register() {
        secondaryFactory = FallbackComputerBehaviour::new;
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CCProxy::registerWithDependency);
    }

    private static void registerWithDependency() {
        primaryFactory = ComputerBehaviour::new;
        registerApi(ShipAPI.class, ShipAPI::new);
        registerApi(ThrustersAPI.class, ThrustersAPI::new);
        registerApi(LinksAPI.class, LinksAPI::new);
    }

    private static void registerApi(Class<? extends ILuaAPI> type, Function<IComputerSystem, ILuaAPI> factory) {
        ComputerCraftAPI.registerAPIFactory(computer ->
            ComputerCraftConfig.isEnabled(type) ? factory.apply(computer) : null);
    }

    private static Function<SmartBlockEntity, ? extends AbstractComputerBehaviour> secondaryFactory;
    private static Function<SmartBlockEntity, ? extends AbstractComputerBehaviour> primaryFactory;

    public static AbstractComputerBehaviour behaviour(SmartBlockEntity blockEntity) {
        if (primaryFactory == null) {
            secondaryFactory = FallbackComputerBehaviour::new;
            return secondaryFactory.apply(blockEntity);
        }
        return primaryFactory.apply(blockEntity);
    }
}
