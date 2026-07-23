package dev.propulsionteam.propulsionsimulated.mixin;

import dev.propulsionteam.propulsionsimulated.registries.PropulsionCreativeTab;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @Shadow
    private Set<ItemStack> displayItemsSearchTab;

    @Inject(method = "buildContents", at = @At("HEAD"), cancellable = true)
    private void createpropulsion$buildContents(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        if ((Object) this != PropulsionCreativeTab.TAB) {
            return;
        }

        List<ItemStack> tabItems = PropulsionCreativeTab.newDisplayItems();
        Set<ItemStack> searchItems = PropulsionCreativeTab.newSearchItems();
        PropulsionCreativeTab.buildContents(tabItems, searchItems);
        this.displayItems = tabItems;
        this.displayItemsSearchTab = searchItems;
        ci.cancel();
    }
}
