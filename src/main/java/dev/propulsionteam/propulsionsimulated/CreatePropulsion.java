package dev.propulsionteam.propulsionsimulated;

import dev.propulsionteam.propulsionsimulated.registries.*;

import dev.propulsionteam.propulsionsimulated.compat.computercraft.CCProxy;
import dev.propulsionteam.propulsionsimulated.events.ModCapabilityEvents;
import dev.propulsionteam.propulsionsimulated.events.ModSetupEvents;
import dev.propulsionteam.propulsionsimulated.network.PropulsionPackets;
import dev.propulsionteam.propulsionsimulated.particles.ParticleTypes;
import dev.propulsionteam.propulsionsimulated.assemblerstick.item.ModItems;
import com.simibubi.create.compat.Mods;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CreatePropulsion.ID)
public class CreatePropulsion {
    public static final String ID = "createpropulsion";
    private static final String INCOMPATIBLE_MOD_ID = "createpropulsionsimulated";

    public CreatePropulsion(IEventBus modBus, ModContainer modContainer) {
        if (ModList.get().isLoaded(INCOMPATIBLE_MOD_ID)) {
            throw new IllegalStateException(
                    "Create: Propulsion is incompatible with createpropulsionsimulated. "
                            + "Both mods provide the same features and cannot be loaded together.");
        }
        modBus.addListener(ModCapabilityEvents::registerCapabilities);
        modBus.addListener(ModSetupEvents::onCommonSetup);
        //Content
        ParticleTypes.register(modBus);
        PropulsionBlocks.register(modBus);
        PropulsionBlockEntities.register(modBus);
        PropulsionItems.register(modBus);
        ModItems.register(modBus);
        PropulsionSoundEvents.register(modBus);
        PropulsionFluids.register(modBus);
        PropulsionPartialModels.register();
        PropulsionCreativeTab.registerAeronauticsSections();
        modBus.addListener(PropulsionPackets::register);
        PropulsionDisplaySources.register();
        PropulsionSableBridge.init();

        //Compat
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CCProxy::register);

        //Config
        modContainer.registerConfig(ModConfig.Type.COMMON, PropulsionConfig.COMMON_SPEC, ID + "-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, PropulsionConfig.CLIENT_SPEC, ID + "-client.toml");
        PropulsionDefaultStress.init(PropulsionConfig.COMMON_SPEC);
    }

    public static ResourceLocation loc(String loc) {
        return ResourceLocation.fromNamespaceAndPath(ID, loc);
    }
}
