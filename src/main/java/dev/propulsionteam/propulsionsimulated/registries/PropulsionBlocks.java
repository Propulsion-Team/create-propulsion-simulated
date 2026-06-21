package dev.propulsionteam.propulsionsimulated.registries;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.heat.burners.liquid.LiquidBurnerBlock;
import dev.propulsionteam.propulsionsimulated.content.heat.burners.solid.SolidBurnerBlock;
import dev.propulsionteam.propulsionsimulated.content.cable.fe.FeCableBlock;
import dev.propulsionteam.propulsionsimulated.content.cable.hub.CableHubBlock;
import dev.propulsionteam.propulsionsimulated.content.heat.engine.StirlingEngineBlock;
import dev.propulsionteam.propulsionsimulated.content.cable.relay.CableRelayBlock;
import dev.propulsionteam.propulsionsimulated.content.platinum.CoralGeneratorBlock;
import dev.propulsionteam.propulsionsimulated.content.platinum.PlatinumFluidTankBlock;
import dev.propulsionteam.propulsionsimulated.content.platinum.PlatinumFluidTankItem;
import dev.propulsionteam.propulsionsimulated.content.platinum.PlatinumCasingBlock;
import dev.propulsionteam.propulsionsimulated.content.redstone_converter.RedstoneConverterBlock;
import dev.propulsionteam.propulsionsimulated.content.redstone_transmission.RedstoneTransmissionBlock;
import dev.propulsionteam.propulsionsimulated.content.tilt_adapter.AdvancedTiltAdapterBlock;
import dev.propulsionteam.propulsionsimulated.content.tilt_adapter.TiltAdapterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.creative_thruster.CreativeThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.creative_vector_thruster.CreativeVectorThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.ion_thruster.IonThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.liquid_vector_thruster.LiquidVectorThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster.SolidFuelThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.wing.CopycatWingBlock;
import dev.propulsionteam.propulsionsimulated.content.wing.CopycatWingItem;
import dev.propulsionteam.propulsionsimulated.content.wing.WingBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class PropulsionBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreatePropulsion.ID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(CreatePropulsion.ID);

    public static final DeferredBlock<ThrusterBlock> THRUSTER_BLOCK = BLOCKS.register("thruster",
        () -> new ThrusterBlock(Block.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
            .sound(SoundType.METAL).strength(5.5f, 4.0f).noOcclusion()));
    public static final DeferredBlock<CreativeThrusterBlock> CREATIVE_THRUSTER_BLOCK = BLOCKS.register("creative_thruster",
        () -> new CreativeThrusterBlock(Block.Properties.of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL).strength(5.5f, 4.0f).noOcclusion()));
    public static final DeferredBlock<IonThrusterBlock> ION_THRUSTER_BLOCK = BLOCKS.register("ion_thruster",
        () -> new IonThrusterBlock(Block.Properties.of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL).strength(5.5f, 4.0f).noOcclusion()));
    public static final DeferredBlock<VectorThrusterBlock> VECTOR_THRUSTER_BLOCK = BLOCKS.register("vector_thruster",
        () -> new VectorThrusterBlock(Block.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
            .sound(SoundType.METAL).strength(5.5f, 4.0f).noOcclusion()));
    public static final DeferredBlock<LiquidVectorThrusterBlock> LIQUID_VECTOR_THRUSTER_BLOCK = BLOCKS.register("liquid_vector_thruster",
        () -> new LiquidVectorThrusterBlock(Block.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
            .sound(SoundType.METAL).strength(5.5f, 4.0f).noOcclusion()));
    public static final DeferredBlock<SolidFuelThrusterBlock> SOLID_FUEL_THRUSTER_BLOCK = BLOCKS.register("solid_fuel_thruster",
        () -> new SolidFuelThrusterBlock(Block.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops()
            .sound(SoundType.METAL).strength(5.5f, 4.0f).noOcclusion()));
    public static final DeferredBlock<CreativeVectorThrusterBlock> CREATIVE_VECTOR_THRUSTER_BLOCK = BLOCKS.register("creative_vector_thruster",
        () -> new CreativeVectorThrusterBlock(Block.Properties.of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL).strength(5.5f, 4.0f).noOcclusion()));
    public static final DeferredBlock<RedstoneConverterBlock> REDSTONE_CONVERTER_BLOCK = BLOCKS.register("redstone_converter",
        () -> new RedstoneConverterBlock(Block.Properties.of().mapColor(MapColor.METAL)
            .sound(SoundType.METAL).instabreak()));
    public static final DeferredBlock<RedstoneTransmissionBlock> REDSTONE_TRANSMISSION_BLOCK = BLOCKS.register("redstone_transmission",
        () -> new RedstoneTransmissionBlock(Block.Properties.of().mapColor(MapColor.PODZOL)
            .sound(SoundType.METAL).strength(2.5f, 2.0f).noOcclusion()));
    public static final DeferredBlock<SolidBurnerBlock> SOLID_BURNER = BLOCKS.register("solid_burner",
        () -> new SolidBurnerBlock(Block.Properties.of().mapColor(MapColor.STONE).sound(SoundType.COPPER)
            .requiresCorrectToolForDrops().strength(2.5f, 2.0f).lightLevel(s -> s.getValue(SolidBurnerBlock.LIT) ? 13 : 0)));
    public static final DeferredBlock<LiquidBurnerBlock> LIQUID_BURNER = BLOCKS.register("liquid_burner",
        () -> new LiquidBurnerBlock(Block.Properties.of().noOcclusion().mapColor(MapColor.STONE).sound(SoundType.COPPER)
            .requiresCorrectToolForDrops().strength(2.75f, 2.0f)));
    public static final DeferredBlock<StirlingEngineBlock> STIRLING_ENGINE_BLOCK = BLOCKS.register("stirling_engine",
        () -> new StirlingEngineBlock(Block.Properties.of().mapColor(MapColor.STONE).sound(SoundType.COPPER)
            .requiresCorrectToolForDrops().strength(2.5f, 2.0f).noOcclusion()));
    public static final DeferredBlock<TiltAdapterBlock> TILT_ADAPTER_BLOCK = BLOCKS.register("tilt_adapter",
        () -> new TiltAdapterBlock(Block.Properties.of().mapColor(MapColor.PODZOL)
            .sound(SoundType.METAL).strength(2.5f, 2.0f).noOcclusion()));
    public static final DeferredBlock<AdvancedTiltAdapterBlock> ADVANCED_TILT_ADAPTER_BLOCK = BLOCKS.register("advanced_tilt_adapter",
        () -> new AdvancedTiltAdapterBlock(Block.Properties.of().mapColor(MapColor.PODZOL)
            .sound(SoundType.METAL).requiresCorrectToolForDrops().strength(2.5f, 2.0f).noOcclusion()));

    public static final DeferredBlock<WingBlock> WING_BLOCK = BLOCKS.register("wing",
        () -> new WingBlock(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.COPPER)
            .strength(1.5f, 2.0f).noOcclusion()));
    public static final DeferredBlock<WingBlock> TEMPERED_WING_BLOCK = BLOCKS.register("tempered_wing",
        () -> new WingBlock(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.COPPER)
            .strength(1.5f, 2.0f).noOcclusion()));
    public static final DeferredBlock<CopycatWingBlock> COPYCAT_WING = BLOCKS.register("copycat_wing",
        () -> new CopycatWingBlock(Block.Properties.of().strength(1.5f, 2.0f), 4));
    public static final DeferredBlock<CopycatWingBlock> COPYCAT_WING_8 = BLOCKS.register("copycat_wing_8",
        () -> new CopycatWingBlock(Block.Properties.of().strength(1.5f, 2.0f), 8));
    public static final DeferredBlock<CopycatWingBlock> COPYCAT_WING_12 = BLOCKS.register("copycat_wing_12",
        () -> new CopycatWingBlock(Block.Properties.of().strength(1.5f, 2.0f), 12));
    public static final DeferredBlock<Block> PLATINUM_ORE = BLOCKS.register("platinum_ore",
        () -> new Block(Block.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE)
            .requiresCorrectToolForDrops().strength(3.0f, 3.0f)));
    public static final DeferredBlock<Block> DEEPSLATE_PLATINUM_ORE = BLOCKS.register("deepslate_platinum_ore",
        () -> new Block(Block.Properties.of().mapColor(MapColor.DEEPSLATE).sound(SoundType.DEEPSLATE)
            .requiresCorrectToolForDrops().strength(4.5f, 3.0f)));
    public static final DeferredBlock<Block> PLATINUM_BLOCK = BLOCKS.register("platinum_block",
        () -> new Block(Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
            .requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
    public static final DeferredBlock<Block> RAW_PLATINUM_BLOCK = BLOCKS.register("raw_platinum_block",
        () -> new Block(Block.Properties.of().mapColor(MapColor.RAW_IRON).sound(SoundType.STONE)
            .requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
    public static final DeferredBlock<PlatinumCasingBlock> PLATINUM_CASING = BLOCKS.register("platinum_casing",
        () -> new PlatinumCasingBlock(Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
            .requiresCorrectToolForDrops().strength(5.0f, 6.0f)));
    public static final DeferredBlock<PlatinumFluidTankBlock> PLATINUM_FLUID_TANK = BLOCKS.register("platinum_fluid_tank",
        () -> new PlatinumFluidTankBlock(Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.COPPER)
            .requiresCorrectToolForDrops().strength(2.5f, 2.0f).noOcclusion().isRedstoneConductor((s, l, p) -> true)));
    public static final DeferredBlock<CoralGeneratorBlock> CORAL_GENERATOR = BLOCKS.register("coral_generator",
        () -> new CoralGeneratorBlock(Block.Properties.of().mapColor(MapColor.COLOR_CYAN).sound(SoundType.STONE)
            .requiresCorrectToolForDrops().strength(3.5f, 3.0f).noOcclusion()));
    public static final DeferredBlock<FeCableBlock> FE_CABLE = BLOCKS.register("cable",
        () -> new FeCableBlock(Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
            .requiresCorrectToolForDrops().strength(1.75f, 2.0f).noOcclusion()));
    public static final DeferredBlock<CableHubBlock> CABLE_HUB = BLOCKS.register("cable_hub",
        () -> new CableHubBlock(Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
            .requiresCorrectToolForDrops().strength(2.5f, 3.5f).noOcclusion()));
    public static final DeferredBlock<CableRelayBlock> CABLE_RELAY = BLOCKS.register("cable_relay",
        () -> new CableRelayBlock(Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
            .requiresCorrectToolForDrops().strength(2.5f, 3.5f)));

    static {
        registerDefaultBlockItem("thruster", THRUSTER_BLOCK);
        registerBlockItem("creative_thruster", CREATIVE_THRUSTER_BLOCK, new BlockItem.Properties().rarity(Rarity.EPIC));
        registerBlockItem("ion_thruster", ION_THRUSTER_BLOCK, new BlockItem.Properties().rarity(Rarity.UNCOMMON));
        registerBlockItem("vector_thruster", VECTOR_THRUSTER_BLOCK, new BlockItem.Properties().rarity(Rarity.UNCOMMON));
        registerBlockItem("liquid_vector_thruster", LIQUID_VECTOR_THRUSTER_BLOCK, new BlockItem.Properties().rarity(Rarity.UNCOMMON));
        registerDefaultBlockItem("solid_fuel_thruster", SOLID_FUEL_THRUSTER_BLOCK);
        registerBlockItem("creative_vector_thruster", CREATIVE_VECTOR_THRUSTER_BLOCK, new BlockItem.Properties().rarity(Rarity.EPIC));
        registerDefaultBlockItem("redstone_converter", REDSTONE_CONVERTER_BLOCK);
        registerDefaultBlockItem("redstone_transmission", REDSTONE_TRANSMISSION_BLOCK);
        registerDefaultBlockItem("solid_burner", SOLID_BURNER);
        registerDefaultBlockItem("liquid_burner", LIQUID_BURNER);
        registerDefaultBlockItem("stirling_engine", STIRLING_ENGINE_BLOCK);
        registerDefaultBlockItem("tilt_adapter", TILT_ADAPTER_BLOCK);
        registerDefaultBlockItem("advanced_tilt_adapter", ADVANCED_TILT_ADAPTER_BLOCK);
        registerDefaultBlockItem("wing", WING_BLOCK);
        registerDefaultBlockItem("tempered_wing", TEMPERED_WING_BLOCK);
        BLOCK_ITEMS.register("copycat_wing", () -> new CopycatWingItem(COPYCAT_WING.get(), new BlockItem.Properties()));
        BLOCK_ITEMS.register("copycat_wing_8", () -> new CopycatWingItem(COPYCAT_WING_8.get(), new BlockItem.Properties()));
        BLOCK_ITEMS.register("copycat_wing_12", () -> new CopycatWingItem(COPYCAT_WING_12.get(), new BlockItem.Properties()));
        registerDefaultBlockItem("platinum_ore", PLATINUM_ORE);
        registerDefaultBlockItem("deepslate_platinum_ore", DEEPSLATE_PLATINUM_ORE);
        registerDefaultBlockItem("platinum_block", PLATINUM_BLOCK);
        registerDefaultBlockItem("raw_platinum_block", RAW_PLATINUM_BLOCK);
        registerDefaultBlockItem("platinum_casing", PLATINUM_CASING);
        BLOCK_ITEMS.register("platinum_fluid_tank", () -> new PlatinumFluidTankItem(PLATINUM_FLUID_TANK.get(), new BlockItem.Properties()));
        registerBlockItem("coral_generator", CORAL_GENERATOR, new BlockItem.Properties().rarity(Rarity.RARE));
        registerDefaultBlockItem("cable", FE_CABLE);
        registerDefaultBlockItem("cable_hub", CABLE_HUB);
        registerDefaultBlockItem("cable_relay", CABLE_RELAY);

        PropulsionDefaultStress.setImpact(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "redstone_transmission"), 0, false);
        PropulsionDefaultStress.setImpact(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "tilt_adapter"), 0, false);
        PropulsionDefaultStress.setImpact(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "advanced_tilt_adapter"), 0, false);
    }

    private static <T extends Block> void registerDefaultBlockItem(String name, DeferredBlock<T> block) {
        registerBlockItem(name, block, new BlockItem.Properties());
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block, BlockItem.Properties properties) {
        BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), properties));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ITEMS.register(modBus);
    }
}
