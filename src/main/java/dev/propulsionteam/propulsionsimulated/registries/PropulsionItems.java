package dev.propulsionteam.propulsionsimulated.registries;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.assemblerstick.item.CreateStyleTooltipItem;
import dev.propulsionteam.propulsionsimulated.content.toolgun.ToolgunItem;
import dev.propulsionteam.propulsionsimulated.content.propulsion_core.VectorCoreItem;
import dev.propulsionteam.propulsionsimulated.content.super_bucket.SuperBucketItem;
//import dev.propulsionteam.propulsionsimulated.content.cable.CableSpoolItem;
import dev.propulsionteam.propulsionsimulated.utility.BurnableItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PropulsionItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreatePropulsion.ID);

    public static final DeferredHolder<Item, BurnableItem> PINE_RESIN =
        ITEMS.register("pine_resin", () -> new BurnableItem(new Item.Properties(), 1200));
    
    public static final DeferredHolder<Item, Item> TURPENTINE_BUCKET =
        ITEMS.register("turpentine_bucket", () -> new net.minecraft.world.item.BucketItem(PropulsionFluids.TURPENTINE.get(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<Item, Item> CORAL_BUCKET =
        ITEMS.register("coral_bucket", () -> new net.minecraft.world.item.BucketItem(PropulsionFluids.CORAL.get(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<Item, Item> OXIDIZER_BUCKET =
        ITEMS.register("oxidizer_bucket", () -> new net.minecraft.world.item.BucketItem(PropulsionFluids.OXIDIZER.get(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<Item, Item> HYDRAZINE_BUCKET =
        ITEMS.register("hydrazine_bucket", () -> new net.minecraft.world.item.BucketItem(PropulsionFluids.HYDRAZINE.get(), new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1)));
    public static final DeferredHolder<Item, Item> PLATINUM_INGOT =
        ITEMS.register("platinum_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> VECTOR_CORE =
        ITEMS.register("vector_core", () -> new VectorCoreItem(new Item.Properties().rarity(Rarity.EPIC).durability(1200)));
    public static final DeferredHolder<Item, SequencedAssemblyItem> UNFINISHED_VECTOR_CORE =
        ITEMS.register("unfinished_vector_core", () -> new SequencedAssemblyItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PLATINUM_NUGGET =
        ITEMS.register("platinum_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PLATINUM_SHEET =
        ITEMS.register("platinum_sheet", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_PLATINUM =
        ITEMS.register("raw_platinum", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRUSHED_RAW_PLATINUM =
        ITEMS.register("crushed_raw_platinum", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> HUSH_KIT =
        ITEMS.register("hush_kit", () -> new CreateStyleTooltipItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TOOLGUN =
        ITEMS.register("toolgun", () -> new ToolgunItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, SuperBucketItem> SUPER_BUCKET =
        ITEMS.register("super_bucket", () -> new SuperBucketItem(new Item.Properties().stacksTo(1)));
    // public static final DeferredHolder<Item, CableSpoolItem> CABLE_SPOOL =
    //     ITEMS.register("cable_spool", () -> new CableSpoolItem(new Item.Properties().stacksTo(1)));

    public static java.util.Collection<DeferredHolder<Item, ? extends Item>> getEntries() {
        return ITEMS.getEntries();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
