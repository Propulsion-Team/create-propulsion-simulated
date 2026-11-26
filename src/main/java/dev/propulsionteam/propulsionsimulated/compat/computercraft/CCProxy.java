package dev.propulsionteam.propulsionsimulated.compat.computercraft;

import java.util.function.Function;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.FallbackComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import dan200.computercraft.api.ComputerCraftAPI;

public class CCProxy {
    public static void register() {
        secondaryFactory = FallbackComputerBehaviour::new;
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CCProxy::registerWithDependency);
    }

    private static void registerWithDependency() {
        primaryFactory = ComputerBehaviour::new;
        ComputerCraftAPI.registerAPIFactory(ShipAPI::new);
        ComputerCraftAPI.registerAPIFactory(ThrustersAPI::new);
        ComputerCraftAPI.registerAPIFactory(LinksAPI::new);
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
