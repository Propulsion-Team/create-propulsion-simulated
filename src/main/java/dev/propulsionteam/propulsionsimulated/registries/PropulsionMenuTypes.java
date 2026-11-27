package dev.propulsionteam.propulsionsimulated.registries;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.lidar.LidarMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PropulsionMenuTypes {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, CreatePropulsion.ID);

    public static final DeferredHolder<MenuType<?>, MenuType<LidarMenu>> LIDAR = MENU_TYPES.register("lidar",
        () -> IMenuTypeExtension.create((id, inv, buf) -> new LidarMenu(PropulsionMenuTypes.LIDAR.get(), id, inv, buf)));

    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
