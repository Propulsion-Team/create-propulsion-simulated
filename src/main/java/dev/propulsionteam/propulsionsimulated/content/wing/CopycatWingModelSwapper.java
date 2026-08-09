package dev.propulsionteam.propulsionsimulated.content.wing;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import com.simibubi.create.foundation.model.ModelSwapper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CopycatWingModelSwapper {
    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        ModelSwapper.swapModels(
            event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.COPYCAT_WING.get()),
            CopycatWingModel.create(4)
        );
        ModelSwapper.swapModels(
            event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.COPYCAT_WING_8.get()),
            CopycatWingModel.create(8)
        );
        ModelSwapper.swapModels(
            event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.COPYCAT_WING_12.get()),
            CopycatWingModel.create(12)
        );
        ModelSwapper.swapModels(
            event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.SYMMETRIC_COPYCAT_WING.get()),
            CopycatWingModel.create(4)
        );
        ModelSwapper.swapModels(
            event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.SYMMETRIC_COPYCAT_WING_8.get()),
            CopycatWingModel.create(8)
        );
        ModelSwapper.swapModels(
            event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.SYMMETRIC_COPYCAT_WING_12.get()),
            CopycatWingModel.create(12)
        );
    }
}

