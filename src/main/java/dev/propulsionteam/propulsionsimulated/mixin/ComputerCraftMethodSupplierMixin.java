package dev.propulsionteam.propulsionsimulated.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dan200.computercraft.core.methods.NamedMethod;
import dev.propulsionteam.propulsionsimulated.compat.computercraft.ComputerCraftConfig;
import dev.propulsionteam.propulsionsimulated.mixin.plugin.MixinIf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;


// Hides Lua methods disabled in the config. The returned list is filtered rather than the cached one,
// so config changes apply the next time a computer looks up the methods.

@Pseudo
@MixinIf("is_computercraft_loaded")
@Mixin(targets = "dan200.computercraft.core.asm.MethodSupplierImpl", remap = false)
public class ComputerCraftMethodSupplierMixin {
    @ModifyReturnValue(method = "getMethods", at = @At("RETURN"), remap = false)
    private List<NamedMethod<?>> createpropulsion$filterDisabledMethods(List<NamedMethod<?>> methods,
            Class<?> type) {
        List<NamedMethod<?>> enabled = methods.stream()
                .filter(method -> ComputerCraftConfig.isMethodEnabled(type, method.name()))
                .toList();
        return enabled.size() == methods.size() ? methods : enabled;
    }
}
