package dev.propulsionteam.propulsionsimulated.mixin;

import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.mixin.plugin.MixinIf;
import net.minecraft.core.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(targets = "rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.FluidShellProjectile")
@MixinIf("is_createbigcannons_loaded")
public abstract class FluidShellProjectileMixin {
    private static final ResourceLocation CORAL_ID = ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "coral");
    private static final float TNT_POWER = 4.0f;
    private static final float CORAL_MB_PER_TNT = 500.0f;

    @Inject(method = "detonate", at = @At("HEAD"), cancellable = true)
    private void createpropulsion$detonateCoralAsExplosion(Position position, CallbackInfo ci) {
        try {
            var fluidStackField = this.getClass().getDeclaredField("fluidStack");
            fluidStackField.setAccessible(true);
            Object endFluidStack = fluidStackField.get(this);
            if (endFluidStack == null) {
                return;
            }

            Method fluidMethod = endFluidStack.getClass().getMethod("fluid");
            Method amountMethod = endFluidStack.getClass().getMethod("amount");

            Object fluidObj = fluidMethod.invoke(endFluidStack);
            Object amountObj = amountMethod.invoke(endFluidStack);
            if (!(fluidObj instanceof Fluid fluid) || !(amountObj instanceof Integer amountMb)) {
                return;
            }

            if (amountMb <= 0) {
                return;
            }

            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            if (!CORAL_ID.equals(fluidId)) {
                return;
            }

            float tntEquivalent = amountMb / CORAL_MB_PER_TNT;
            if (tntEquivalent <= 0.0f) {
                return;
            }

            float explosionPower = tntEquivalent * TNT_POWER;
            Level level = ((Entity) (Object) this).level();
            level.explode(
                    null,
                    position.x(),
                    position.y(),
                    position.z(),
                    explosionPower,
                    Level.ExplosionInteraction.TNT
            );
            ci.cancel();
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
