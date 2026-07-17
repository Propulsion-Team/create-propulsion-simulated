package dev.propulsionteam.propulsionsimulated.events;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorFuelManager;
import dev.propulsionteam.propulsionsimulated.content.thruster.SolidThrusterFuelManager;
import dev.propulsionteam.propulsionsimulated.content.thruster.ThrusterFuelManager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

@EventBusSubscriber(modid = CreatePropulsion.ID)
public final class PropulsionModConfigEvents {

    private PropulsionModConfigEvents() {}

    @SubscribeEvent
    public static void onCommonConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != PropulsionConfig.COMMON_SPEC) {
            return;
        }
        ThrusterFuelManager.rebuildThrusterFuelsAfterCommonConfigReload();
        SolidThrusterFuelManager.rebuildAfterCommonConfigReload();
        CoralGeneratorFuelManager.rebuildCoralFuelsAfterCommonConfigReload();
    }
}
