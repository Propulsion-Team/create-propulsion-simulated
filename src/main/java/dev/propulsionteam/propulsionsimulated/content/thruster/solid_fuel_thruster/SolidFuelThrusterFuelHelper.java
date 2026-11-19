package dev.propulsionteam.propulsionsimulated.content.thruster.solid_fuel_thruster;

import com.simibubi.create.api.data.datamaps.BlazeBurnerFuel;
import com.simibubi.create.api.registry.CreateDataMaps;
import dev.propulsionteam.propulsionsimulated.content.thruster.SolidThrusterFuelManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Burn-time resolution aligned with Create Aeronautics' portable engine:
 * superheated blaze-burner fuels, and effectively infinite creative blaze cake.
 */
public final class SolidFuelThrusterFuelHelper {
    /** Same threshold as {@code PortableEngineBlockEntity.INFINITE_THRESHOLD}. */
    public static final int INFINITE_THRESHOLD = 51_840_000;

    private static final ResourceLocation CREATIVE_BLAZE_CAKE =
        ResourceLocation.fromNamespaceAndPath("create", "creative_blaze_cake");

    private SolidFuelThrusterFuelHelper() {}

    public static boolean isCreativeBlazeCake(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(CREATIVE_BLAZE_CAKE);
    }

    public static boolean isInfiniteFuel(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isCreativeBlazeCake(stack)) {
            return true;
        }
        return resolveTotalBurnTicks(stack) >= INFINITE_THRESHOLD;
    }

    public static boolean isInfiniteBurnTime(int burnTime) {
        return burnTime >= INFINITE_THRESHOLD;
    }

    public static int getSuperheatedBurnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        BlazeBurnerFuel fuel = stack.getItemHolder().getData(CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS);
        return fuel == null ? 0 : fuel.burnTime();
    }

    public static boolean isSuperheatedFuel(ItemStack stack) {
        return getSuperheatedBurnTime(stack) > 0;
    }

    public static int resolveTotalBurnTicks(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (isCreativeBlazeCake(stack)) {
            return INFINITE_THRESHOLD;
        }
        int superheated = getSuperheatedBurnTime(stack);
        if (superheated > 0) {
            return superheated;
        }
        return SolidThrusterFuelManager.resolveBurnTicks(stack);
    }

    @Nullable
    public static String formatBurnTime(int ticks) {
        if (ticks <= 0) {
            return null;
        }
        if (ticks >= INFINITE_THRESHOLD) {
            return null;
        }
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append('h').append(' ');
        }
        if (minutes > 0 || hours > 0) {
            sb.append(minutes).append('m').append(' ');
        }
        sb.append(seconds).append('s');
        return sb.toString().trim();
    }
}
