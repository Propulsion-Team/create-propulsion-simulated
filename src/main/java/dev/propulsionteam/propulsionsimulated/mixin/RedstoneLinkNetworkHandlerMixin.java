package dev.propulsionteam.propulsionsimulated.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import dev.propulsionteam.propulsionsimulated.utility.redstone_link.ShipScopedLinks;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Create pushes one power value to every receiver in a network. When a ship-scoped computer link is
// transmitting in the network, recompute the value per receiver so links off that ship don't hear it.
// Wraps setReceivedStrength rather than the range check, which Sable already redirects.
@Mixin(RedstoneLinkNetworkHandler.class)
public abstract class RedstoneLinkNetworkHandlerMixin {
    @WrapOperation(method = "updateNetworkOf", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;setReceivedStrength(I)V"))
    private void propulsion$scopeReceivers(IRedstoneLinkable receiver, int power, Operation<Void> original,
                                           @Local(argsOnly = true) LevelAccessor world) {
        original.call(receiver, ShipScopedLinks.adjustReceived(
            (RedstoneLinkNetworkHandler) (Object) this, world, receiver, power));
    }

    // Same, for the actor itself when it is a LinkBehaviour (a separate invokevirtual call site).
    @WrapOperation(method = "updateNetworkOf", at = @At(value = "INVOKE",
        target = "Lcom/simibubi/create/content/redstone/link/LinkBehaviour;setReceivedStrength(I)V"))
    private void propulsion$scopeActor(LinkBehaviour receiver, int power, Operation<Void> original,
                                       @Local(argsOnly = true) LevelAccessor world) {
        original.call(receiver, ShipScopedLinks.adjustReceived(
            (RedstoneLinkNetworkHandler) (Object) this, world, receiver, power));
    }
}
