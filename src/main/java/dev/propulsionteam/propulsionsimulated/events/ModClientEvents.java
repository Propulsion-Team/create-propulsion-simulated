package dev.propulsionteam.propulsionsimulated.events;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.model.ModelSwapper;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.heat.burners.liquid.LiquidBurnerRenderer;
import dev.propulsionteam.propulsionsimulated.content.platinum.PlatinumFluidTankModel;
import dev.propulsionteam.propulsionsimulated.content.platinum.PlatinumFluidVesselModel;
import dev.propulsionteam.propulsionsimulated.content.platinum.PlatinumFluidVesselRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterRenderer;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlocks;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionSpriteShifts;
import dev.propulsionteam.propulsionsimulated.content.heat.burners.liquid.LiquidBurnerVisual;
import dev.propulsionteam.propulsionsimulated.content.heat.engine.StirlingEngineRenderer;
import dev.propulsionteam.propulsionsimulated.content.heat.engine.StirlingEngineVisual;
import dev.propulsionteam.propulsionsimulated.ponder.DeltaPonderPlugin;
import dev.propulsionteam.propulsionsimulated.content.redstone_transmission.RedstoneTransmissionRenderer;
import dev.propulsionteam.propulsionsimulated.content.redstone_transmission.RedstoneTransmissionVisual;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionBlockEntities;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionInstanceTypes;
import dev.propulsionteam.propulsionsimulated.content.tilt_adapter.TiltAdapterRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster.LiquidVectorThrusterRenderer;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterRenderer;
import dev.propulsionteam.propulsionsimulated.registries.PropulsionFluids;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = CreatePropulsion.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Removed colorized optical lens handling.
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        // Removed assembly gauge overlay.
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.parse("minecraft:block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.parse("minecraft:block/water_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFFD69E49;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return 0xFFD69E49;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return 0xFFD69E49;
            }
        }, PropulsionFluids.TURPENTINE_TYPE);

        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "block/coral_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "block/coral_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return 0xFFFFFFFF;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return 0xFFFFFFFF;
            }
        }, PropulsionFluids.CORAL_TYPE);

        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.parse("minecraft:block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.parse("minecraft:block/water_flow");
            }

            @Override
            public int getTintColor() {
                return 0xFF88CCFF;
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return 0xFF88CCFF;
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return 0xFF88CCFF;
            }
        }, PropulsionFluids.OXIDIZER_TYPE);
    }

    @SubscribeEvent
    public static void clientInit(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(PropulsionFluids.TURPENTINE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(PropulsionFluids.FLOWING_TURPENTINE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(PropulsionFluids.CORAL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(PropulsionFluids.FLOWING_CORAL.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(PropulsionFluids.OXIDIZER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(PropulsionFluids.FLOWING_OXIDIZER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(PropulsionBlocks.PLATINUM_FLUID_TANK.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(PropulsionBlocks.PLATINUM_FLUID_VESSEL.get(), RenderType.cutoutMipped());
        });

        PonderIndex.addPlugin(new DeltaPonderPlugin());
        PropulsionInstanceTypes.register();

        CreateClient.CASING_CONNECTIVITY.makeCasing(PropulsionBlocks.PLATINUM_CASING.get(), PropulsionSpriteShifts.PLATINUM_CASING_TEXTURE);

        SimpleBlockEntityVisualizer.builder(PropulsionBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY.get())
            .factory(StirlingEngineVisual::new)
            .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
            .apply();

        SimpleBlockEntityVisualizer.builder(PropulsionBlockEntities.REDSTONE_TRANSMISSION_BLOCK_ENTITY.get())
            .factory(RedstoneTransmissionVisual::new)
            .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
            .apply();

        SimpleBlockEntityVisualizer.builder(PropulsionBlockEntities.LIQUID_BURNER_BLOCK_ENTITY.get())
            .factory(LiquidBurnerVisual::new)
            .skipVanillaRender(be -> VisualizationManager.supportsVisualization(be.getLevel()))
            .apply();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PropulsionBlockEntities.STIRLING_ENGINE_BLOCK_ENTITY.get(), StirlingEngineRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.REDSTONE_TRANSMISSION_BLOCK_ENTITY.get(), RedstoneTransmissionRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.CREATIVE_THRUSTER_BLOCK_ENTITY.get(), CreativeThrusterRenderer::new);

        event.registerBlockEntityRenderer(PropulsionBlockEntities.THRUSTER_BLOCK_ENTITY.get(), ThrusterRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.ION_THRUSTER_BLOCK_ENTITY.get(), IonThrusterRenderer::new);

        event.registerBlockEntityRenderer(PropulsionBlockEntities.CREATIVE_VECTOR_THRUSTER_BLOCK_ENTITY.get(), IonThrusterRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.LIQUID_VECTOR_THRUSTER_BLOCK_ENTITY.get(), LiquidVectorThrusterRenderer::new);

        event.registerBlockEntityRenderer(PropulsionBlockEntities.SOLID_FUEL_THRUSTER_BLOCK_ENTITY.get(), SolidFuelThrusterRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.LIQUID_BURNER_BLOCK_ENTITY.get(), LiquidBurnerRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.TILT_ADAPTER_BLOCK_ENTITY.get(), TiltAdapterRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.ADVANCED_TILT_ADAPTER_BLOCK_ENTITY.get(), TiltAdapterRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.PLATINUM_FLUID_TANK_BLOCK_ENTITY.get(), FluidTankRenderer::new);
        event.registerBlockEntityRenderer(PropulsionBlockEntities.PLATINUM_FLUID_VESSEL_BLOCK_ENTITY.get(), PlatinumFluidVesselRenderer::new);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        EncasedCTBehaviour behaviour = new EncasedCTBehaviour(PropulsionSpriteShifts.PLATINUM_CASING_TEXTURE);
        ModelSwapper.swapModels(event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.PLATINUM_CASING.get()),
            model -> new CTModel(model, behaviour));
        ModelSwapper.swapModels(event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.PLATINUM_FLUID_TANK.get()),
            PlatinumFluidTankModel::new);
        ModelSwapper.swapModels(event.getModels(),
            ModelSwapper.getAllBlockStateModelLocations(PropulsionBlocks.PLATINUM_FLUID_VESSEL.get()),
            PlatinumFluidVesselModel::new);
    }
}
