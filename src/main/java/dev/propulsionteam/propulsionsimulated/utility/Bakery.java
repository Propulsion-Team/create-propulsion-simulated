package dev.propulsionteam.propulsionsimulated.utility;

import java.util.Locale;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = CreatePropulsion.ID, value = Dist.CLIENT)
public class Bakery {
    public static final BakedModel[] BAKED_COMPASS_MODELS = new BakedModel[32];
    public static final ModelResourceLocation[] COMPASS_MODELS = new ModelResourceLocation[32];
    static {
        for (int i = 0; i < 32; ++i) {
            String modelPath;
            //Cuz minecraft was developed by corporation who does not know about number 16
            if (i == 16) {
                modelPath = "compass";
            } else {
                // i really had to make this use root language because minecrafts languages is so ass that it doesnt turn arabic numbers to normal ones.. like tf?
                modelPath = String.format(Locale.ROOT, "compass_%02d", i);
            }
            // NeoForge 1.21 requires side-loaded models to use the standalone variant.
            COMPASS_MODELS[i] = ModelResourceLocation.vanilla(modelPath, "standalone");
        }
    }
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (int i = 0; i < 32; ++i) {
            event.register(COMPASS_MODELS[i]);
        }
    }

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        ModelManager modelManager = event.getModelManager();
        for (int i = 0; i < 32; i++) {
            BAKED_COMPASS_MODELS[i] = modelManager.getModel(COMPASS_MODELS[i]);
        }
    }
}
