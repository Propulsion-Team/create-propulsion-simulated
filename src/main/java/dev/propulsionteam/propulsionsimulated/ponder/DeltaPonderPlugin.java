package dev.propulsionteam.propulsionsimulated.ponder;

import javax.annotation.Nonnull;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class DeltaPonderPlugin implements PonderPlugin {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        final PonderSceneRegistrationHelper<Block> HELPER = helper.withKeyFunction(BuiltInRegistries.BLOCK::getKey);
        //Burners
        HELPER.forComponents(PropulsionBlocks.SOLID_BURNER.get()).addStoryBoard("solid_burner", BurnerScenes::solidBurner);
        HELPER.forComponents(PropulsionBlocks.LIQUID_BURNER.get()).addStoryBoard("liquid_burner", BurnerScenes::liquidBurner);
        //Stirling engine
        HELPER.forComponents(PropulsionBlocks.STIRLING_ENGINE_BLOCK.get())
                .addStoryBoard("stirling_engine_solid", StirlingEngineScene::stirlingEngine)
                .addStoryBoard("stirling_engine_liquid", StirlingEngineScene::stirlingEngineLiquid);
        //Transmission
        HELPER.forComponents(PropulsionBlocks.REDSTONE_TRANSMISSION_BLOCK.get())
                .addStoryBoard("redstone_transmission", TransmissionScenes::directControl)
                .addStoryBoard("redstone_transmission", TransmissionScenes::incrementalControl);
        //Thruster
        HELPER.forComponents(PropulsionBlocks.THRUSTER_BLOCK.get())
                .addStoryBoard("ponder_thruster_normal", ThrusterScenes::normal)
                .addStoryBoard("ponder_thruster_2x2", ThrusterScenes::multiblock2x2)
                .addStoryBoard("ponder_thruster_3x3", ThrusterScenes::multiblock3x3);
        HELPER.forComponents(PropulsionBlocks.VECTOR_THRUSTER_BLOCK.get())
                .addStoryBoard("ponder_vector_thruster_normal", VectorThrusterFeScenes::vectorThrusterFe);
        HELPER.forComponents(PropulsionBlocks.CREATIVE_VECTOR_THRUSTER_BLOCK.get())
                .addStoryBoard("ponder_creative_vector_thruster", VectorThrusterFeScenes::creativeVectorThruster);
        HELPER.forComponents(PropulsionBlocks.LIQUID_VECTOR_THRUSTER_BLOCK.get())
                .addStoryBoard("ponder_liquid_vector_thruster", VectorThrusterFeScenes::liquidVectorThruster);
    }

    @Override
	public String getModId() {
		return CreatePropulsion.ID;
	}

	@Override
	public void registerScenes(@Nonnull PonderSceneRegistrationHelper<ResourceLocation> helper) {
		register(helper);
	}

    @Override
    public void registerTags(@Nonnull PonderTagRegistrationHelper<ResourceLocation> helper) {
        final PonderTagRegistrationHelper<Block> HELPER = helper.withKeyFunction(BuiltInRegistries.BLOCK::getKey);
        HELPER.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
            .add(PropulsionBlocks.STIRLING_ENGINE_BLOCK.get())
            .add(PropulsionBlocks.VECTOR_THRUSTER_BLOCK.get())
            .add(PropulsionBlocks.CREATIVE_VECTOR_THRUSTER_BLOCK.get())
            .add(PropulsionBlocks.LIQUID_VECTOR_THRUSTER_BLOCK.get());
    }
}