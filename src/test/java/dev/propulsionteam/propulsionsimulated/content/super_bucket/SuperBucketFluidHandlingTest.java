package dev.propulsionteam.propulsionsimulated.content.super_bucket;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperBucketFluidHandlingTest {
    @BeforeAll
    static void bootstrap() {
        // Vanilla bootstrap consults NeoForge's mod list when loading feature flags.
        if (LoadingModList.get() == null) {
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void copyingDoesNotDrainAndPreservesFluidComponents() {
        FluidTank tank = new FluidTank(8000) {
            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                throw new AssertionError("Copying must never drain containers");
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                throw new AssertionError("Copying must never drain containers");
            }
        };
        FluidStack original = new FluidStack(Fluids.WATER, 125);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("Sample"));
        tank.setFluid(original);

        FluidStack copied = SuperBucketFluidHandling.copyFluid(tank);

        assertEquals(125, tank.getFluidAmount());
        assertEquals(1000, copied.getAmount());
        assertTrue(FluidStack.isSameFluidSameComponents(original, copied));
        copied.setAmount(50);
        assertEquals(125, tank.getFluidAmount());
    }

    @Test
    void emptyContainersHaveNothingToCopy() {
        assertTrue(SuperBucketFluidHandling.copyFluid(new FluidTank(8000)).isEmpty());
    }

    @Test
    void fillsWholeContainerWithoutConsumingSelection() {
        FluidTank tank = new FluidTank(64000);
        tank.setFluid(new FluidStack(Fluids.WATER, 123));
        FluidStack selected = new FluidStack(Fluids.WATER, 1000);

        assertEquals(63877, SuperBucketFluidHandling.fillContainer(tank, selected));
        assertEquals(64000, tank.getFluidAmount());
        assertEquals(1000, selected.getAmount());
        assertEquals(0, SuperBucketFluidHandling.fillContainer(tank, selected));
    }

    @Test
    void incompatibleAndInputLockedContainersAreUnchanged() {
        FluidStack selected = new FluidStack(Fluids.WATER, 1000);
        FluidTank incompatible = new FluidTank(8000);
        incompatible.setFluid(new FluidStack(Fluids.LAVA, 500));
        FluidTank locked = new FluidTank(8000, fluid -> false);

        assertEquals(0, SuperBucketFluidHandling.fillContainer(incompatible, selected));
        assertEquals(500, incompatible.getFluidAmount());
        assertEquals(0, SuperBucketFluidHandling.fillContainer(locked, selected));
        assertTrue(locked.isEmpty());
    }

    @Test
    void fillsAllCompatibleTanksAndSkipsOtherFluids() {
        FluidTank[] tanks = {new FluidTank(2000), new FluidTank(5000), new FluidTank(8000)};
        tanks[0].setFluid(new FluidStack(Fluids.LAVA, 100));
        tanks[1].setFluid(new FluidStack(Fluids.WATER, 300));
        IFluidHandler handler = new MultiTankHandler(tanks);

        assertTrue(SuperBucketFluidHandling.copyFluid(handler).is(Fluids.LAVA));
        assertEquals(12700, SuperBucketFluidHandling.fillContainer(handler, new FluidStack(Fluids.WATER, 1000)));
        assertEquals(100, tanks[0].getFluidAmount());
        assertEquals(5000, tanks[1].getFluidAmount());
        assertEquals(8000, tanks[2].getFluidAmount());
    }

    @Test
    void clearsWholeContainerAndDiscardsItsFluid() {
        FluidTank tank = new FluidTank(64000);
        tank.setFluid(new FluidStack(Fluids.WATER, 64000));

        assertEquals(64000, SuperBucketFluidHandling.clearContainer(tank));
        assertTrue(tank.isEmpty());
        assertEquals(0, SuperBucketFluidHandling.clearContainer(tank));
    }

    @Test
    void clearsAllTanksIncludingDifferentFluids() {
        FluidTank[] tanks = {new FluidTank(2000), new FluidTank(5000), new FluidTank(8000)};
        tanks[0].setFluid(new FluidStack(Fluids.LAVA, 100));
        tanks[1].setFluid(new FluidStack(Fluids.WATER, 300));
        tanks[2].setFluid(new FluidStack(Fluids.WATER, 8000));

        assertEquals(8400, SuperBucketFluidHandling.clearContainer(new MultiTankHandler(tanks)));
        for (FluidTank tank : tanks) {
            assertTrue(tank.isEmpty());
        }
    }

    @Test
    void clearingRespectsOutputLocks() {
        FluidTank tank = new FluidTank(8000) {
            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
        tank.setFluid(new FluidStack(Fluids.WATER, 500));

        assertEquals(0, SuperBucketFluidHandling.clearContainer(tank));
        assertEquals(500, tank.getFluidAmount());
    }

    private record MultiTankHandler(FluidTank[] tanks) implements IFluidHandler {
        @Override
        public int getTanks() {
            return tanks.length;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks[tank].getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tanks[tank].getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tanks[tank].isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int filled = 0;
            for (FluidTank tank : tanks) {
                filled += tank.fill(resource.copyWithAmount(resource.getAmount() - filled), action);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            int drained = 0;
            for (FluidTank tank : tanks) {
                drained += tank.drain(resource.copyWithAmount(resource.getAmount() - drained), action).getAmount();
            }
            return resource.copyWithAmount(drained);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            throw new AssertionError("Draining must specify the fluid to support mixed containers");
        }
    }
}
