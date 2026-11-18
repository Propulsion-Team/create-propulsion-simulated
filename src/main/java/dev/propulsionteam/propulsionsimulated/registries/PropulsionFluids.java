package dev.propulsionteam.propulsionsimulated.registries;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class PropulsionFluids {
    private static final String TURPENTINE_DESCRIPTION = "fluid." + CreatePropulsion.ID + ".turpentine";
    private static final String CORAL_DESCRIPTION = "fluid." + CreatePropulsion.ID + ".coral";

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreatePropulsion.ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, CreatePropulsion.ID);

    public static final DeferredHolder<FluidType, FluidType> TURPENTINE_TYPE = FLUID_TYPES.register("turpentine_type",
            () -> new FluidType(FluidType.Properties.create().descriptionId(TURPENTINE_DESCRIPTION).density(500).viscosity(1000)));
    public static final DeferredHolder<FluidType, FluidType> CORAL_TYPE = FLUID_TYPES.register("coral_type",
            () -> new FluidType(FluidType.Properties.create().descriptionId(CORAL_DESCRIPTION).density(1000).viscosity(1200)));
    public static final DeferredHolder<FluidType, FluidType> OXIDIZER_TYPE = FLUID_TYPES.register("oxidizer_type",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid." + CreatePropulsion.ID + ".oxidizer").density(700).viscosity(500)));
    public static final DeferredHolder<FluidType, FluidType> HYDRAZINE_TYPE = FLUID_TYPES.register("hydrazine_type",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid." + CreatePropulsion.ID + ".hydrazine").density(1000).viscosity(1000)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> TURPENTINE = FLUIDS.register("turpentine",
            () -> new ProtectedFlowingFluid.Source(turpentineProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_TURPENTINE = FLUIDS.register("flowing_turpentine",
            () -> new ProtectedFlowingFluid.Flowing(turpentineProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> CORAL = FLUIDS.register("coral",
            () -> new ProtectedFlowingFluid.Source(coralProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_CORAL = FLUIDS.register("flowing_coral",
            () -> new ProtectedFlowingFluid.Flowing(coralProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OXIDIZER = FLUIDS.register("oxidizer",
            () -> new ProtectedFlowingFluid.Source(oxidizerProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_OXIDIZER = FLUIDS.register("flowing_oxidizer",
            () -> new ProtectedFlowingFluid.Flowing(oxidizerProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> HYDRAZINE = FLUIDS.register("hydrazine",
            () -> new ProtectedFlowingFluid.Source(hydrazineProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_HYDRAZINE = FLUIDS.register("flowing_hydrazine",
            () -> new ProtectedFlowingFluid.Flowing(hydrazineProperties()));

    public static final DeferredBlock<LiquidBlock> TURPENTINE_BLOCK = PropulsionBlocks.BLOCKS.register("turpentine",
            () -> new LiquidBlock((FlowingFluid) TURPENTINE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));
    public static final DeferredBlock<LiquidBlock> CORAL_BLOCK = PropulsionBlocks.BLOCKS.register("coral",
            () -> new LiquidBlock((FlowingFluid) CORAL.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));
    public static final DeferredBlock<LiquidBlock> OXIDIZER_BLOCK = PropulsionBlocks.BLOCKS.register("oxidizer",
            () -> new LiquidBlock((FlowingFluid) OXIDIZER.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));
    public static final DeferredBlock<LiquidBlock> HYDRAZINE_BLOCK = PropulsionBlocks.BLOCKS.register("hydrazine",
            () -> new LiquidBlock((FlowingFluid) HYDRAZINE.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }

    private static BaseFlowingFluid.Properties turpentineProperties() {
        return new BaseFlowingFluid.Properties(
                TURPENTINE_TYPE,
                TURPENTINE,
                FLOWING_TURPENTINE
        ).bucket(PropulsionItems.TURPENTINE_BUCKET)
                .block(TURPENTINE_BLOCK)
                .levelDecreasePerBlock(1)
                .tickRate(7)
                .slopeFindDistance(3)
                .explosionResistance(100f);
    }
    private static BaseFlowingFluid.Properties coralProperties() {
        return new BaseFlowingFluid.Properties(
                CORAL_TYPE,
                CORAL,
                FLOWING_CORAL
        ).bucket(PropulsionItems.CORAL_BUCKET)
                .block(CORAL_BLOCK)
                .levelDecreasePerBlock(1)
                .tickRate(7)
                .slopeFindDistance(3)
                .explosionResistance(100f);
    }
    private static BaseFlowingFluid.Properties hydrazineProperties() {
        return new BaseFlowingFluid.Properties(
                HYDRAZINE_TYPE,
                HYDRAZINE,
                FLOWING_HYDRAZINE
        ).bucket(PropulsionItems.HYDRAZINE_BUCKET)
                .block(HYDRAZINE_BLOCK)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .slopeFindDistance(3)
                .explosionResistance(100f);
    }
    private static BaseFlowingFluid.Properties oxidizerProperties() {
        return new BaseFlowingFluid.Properties(
                OXIDIZER_TYPE,
                OXIDIZER,
                FLOWING_OXIDIZER
        ).bucket(PropulsionItems.OXIDIZER_BUCKET)
                .block(OXIDIZER_BLOCK)
                .levelDecreasePerBlock(1)
                .tickRate(5)
                .slopeFindDistance(3)
                .explosionResistance(100f);
    }
}
