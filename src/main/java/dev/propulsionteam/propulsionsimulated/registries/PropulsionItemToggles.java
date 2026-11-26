package dev.propulsionteam.propulsionsimulated.registries;

import dev.propulsionteam.propulsionsimulated.PropulsionConfig;
import dev.propulsionteam.propulsionsimulated.assemblerstick.item.ModItems;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PropulsionItemToggles {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PropulsionItemToggles() {
    }

    private static final Set<String> EXCLUDED = Set.of(
            "unfinished_vector_core",
            "toolgun",
            "tilt_adapter",
            "advanced_tilt_adapter");

    public static Set<String> collectItemPaths() {
        Set<String> paths = new LinkedHashSet<>();
        PropulsionBlocks.BLOCK_ITEMS.getEntries().forEach(holder -> paths.add(holder.getId().getPath()));
        PropulsionItems.getEntries().forEach(holder -> paths.add(holder.getId().getPath()));
        ModItems.ITEMS.getEntries().forEach(holder -> paths.add(holder.getId().getPath()));
        paths.removeAll(EXCLUDED);
        return paths;
    }

    public static List<RecipeHolder<?>> filterRecipes(Collection<RecipeHolder<?>> recipes, HolderLookup.Provider registries) {
        List<RecipeHolder<?>> kept = new ArrayList<>(recipes.size());
        int removed = 0;
        for (RecipeHolder<?> holder : recipes) {
            if (producesDisabledItem(holder, registries)) {
                removed++;
            } else {
                kept.add(holder);
            }
        }
        if (removed == 0) {
            return null;
        }
        LOGGER.info("Disabled {} recipe(s) for items turned off in the Create Propulsion config", removed);
        return kept;
    }

    private static boolean producesDisabledItem(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        ItemStack result;
        try {
            result = holder.value().getResultItem(registries);
        } catch (Exception e) {
            return false;
        }
        if (result == null || result.isEmpty()) {
            return false;
        }
        return !PropulsionConfig.isItemEnabled(BuiltInRegistries.ITEM.getKey(result.getItem()));
    }
}
