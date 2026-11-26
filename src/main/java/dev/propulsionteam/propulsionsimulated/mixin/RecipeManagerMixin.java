package dev.propulsionteam.propulsionsimulated.mixin;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionItemToggles;

import com.google.gson.JsonElement;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

// Drops recipes whose result is an item disabled in the "items" section of the common config.

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Shadow
    public abstract java.util.Collection<RecipeHolder<?>> getRecipes();

    @Shadow
    public abstract void replaceRecipes(Iterable<RecipeHolder<?>> recipes);

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL"))
    private void createpropulsion$removeDisabledItemRecipes(
            Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        List<RecipeHolder<?>> kept = PropulsionItemToggles.filterRecipes(getRecipes(), registries);
        if (kept != null) {
            replaceRecipes(kept);
        }
    }
}
