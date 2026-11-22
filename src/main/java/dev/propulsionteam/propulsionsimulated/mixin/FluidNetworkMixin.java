package dev.propulsionteam.propulsionsimulated.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.content.fluids.FluidNetwork;
import dev.propulsionteam.propulsionsimulated.content.hydropump.HydropumpBlockEntity;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(FluidNetwork.class)
public class FluidNetworkMixin {
    @Shadow Level world;
    @Shadow BlockFace start;
    @Unique private Map<BlockPos, BlockPos> createpropulsion$parents;
    @Unique private final Map<HydropumpBlockEntity, Integer> createpropulsion$simulatedTransfers = new HashMap<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void createpropulsion$resetSimulation(CallbackInfo ci) {
        createpropulsion$simulatedTransfers.clear();
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void createpropulsion$resetPaths(CallbackInfo ci) {
        if (createpropulsion$parents != null) {
            createpropulsion$parents.clear();
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
    private boolean createpropulsion$trackPath(List<BlockFace> queued, Object value, Operation<Boolean> original,
                                             @Local(ordinal = 0) BlockFace blockFace) {
        if (createpropulsion$parents == null) {
            createpropulsion$parents = new HashMap<>();
        }
        BlockFace next = (BlockFace) value;
        createpropulsion$parents.put(next.getPos(), blockFace.getPos());
        return original.call(queued, value);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int createpropulsion$powerTransfer(IFluidHandler handler, FluidStack fluid, IFluidHandler.FluidAction action,
                                             Operation<Integer> original,
                                             @Local Pair<BlockFace, FlowSource> target,
                                             @Local Map<IFluidHandler, Integer> accumulatedFill) {
        List<HydropumpBlockEntity> pumps = new ArrayList<>();
        BlockPos pos = target.getFirst().getPos();
        while (pos != null) {
            if (world.getBlockEntity(pos) instanceof HydropumpBlockEntity pump) {
                if (!pump.canTransport()) {
                    return 0;
                }
                pumps.add(pump);
            }
            if (pos.equals(start.getPos())) {
                break;
            }
            pos = createpropulsion$parents == null ? null : createpropulsion$parents.get(pos);
        }
        int previousFill = action.simulate() ? accumulatedFill.getOrDefault(handler, 0) : 0;
        int limit = fluid.getAmount();
        for (HydropumpBlockEntity pump : pumps) {
            int reserved = action.simulate() ? createpropulsion$simulatedTransfers.getOrDefault(pump, 0) : 0;
            limit = Math.min(limit, previousFill + Math.max(0, pump.getTransferAllowance() - reserved));
        }
        int filled = original.call(handler, fluid.copyWithAmount(limit), action);
        if (action.simulate()) {
            int amount = Math.max(0, filled - previousFill);
            for (HydropumpBlockEntity pump : pumps) {
                createpropulsion$simulatedTransfers.merge(pump, amount, Integer::sum);
            }
        }
        if (action.execute() && filled > 0) {
            pumps.forEach(pump -> pump.onFluidTransferred(filled));
        }
        return filled;
    }
}
