package dev.propulsionteam.propulsionsimulated.content.super_bucket;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

final class SuperBucketFluidHandling {
    private SuperBucketFluidHandling() {
    }

    static FluidStack copyFluid(IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (!fluid.isEmpty()) {
                // Inspect rather than drain: even output-locked containers can be sampled.
                return fluid.copyWithAmount(FluidType.BUCKET_VOLUME);
            }
        }
        return FluidStack.EMPTY;
    }

    static long fillContainer(IFluidHandler handler, FluidStack selected) {
        if (selected.isEmpty()) {
            return 0;
        }
        long filled = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stored = handler.getFluidInTank(tank);
            if (!stored.isEmpty() && !FluidStack.isSameFluidSameComponents(stored, selected)) {
                continue;
            }
            int missing = handler.getTankCapacity(tank) - stored.getAmount();
            if (missing > 0 && handler.isFluidValid(tank, selected)) {
                filled += handler.fill(selected.copyWithAmount(missing), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        return filled;
    }

    static long clearContainer(IFluidHandler handler) {
        long drained = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stored = handler.getFluidInTank(tank);
            if (!stored.isEmpty()) {
                // Request each fluid separately so mixed-fluid containers are emptied too.
                drained += handler.drain(stored.copy(), IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
        }
        return drained;
    }
}
