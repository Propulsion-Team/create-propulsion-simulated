package dev.propulsionteam.propulsionsimulated.content.cable;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CableNetworkManagerTest {
    @Test
    void doesNotTransferEnergyBackIntoTheSameEndpoint() {
        TestEnergyStorage battery = TestEnergyStorage.battery(50, 100, 1);

        int transferred = EnergyDistributor.distribute(
            List.of(endpoint(0, battery)), 1_000);

        assertEquals(0, transferred);
        assertEquals(50, battery.energy);
        assertEquals(0, battery.actualExtractions);
        assertEquals(0, battery.actualReceives);
    }

    @Test
    void doesNotReExtractEnergyReceivedEarlierInTheTick() {
        TestEnergyStorage firstBattery = TestEnergyStorage.battery(50, 100, 1);
        TestEnergyStorage secondBattery = TestEnergyStorage.battery(50, 100, 1);

        int transferred = EnergyDistributor.distribute(List.of(
            endpoint(0, firstBattery),
            endpoint(1, secondBattery)
        ), 1_000);

        assertEquals(1, transferred);
        assertEquals(51, firstBattery.energy);
        assertEquals(49, secondBattery.energy);
        assertEquals(0, firstBattery.actualExtractions);
        assertEquals(1, secondBattery.actualExtractions);
        assertEquals(1, firstBattery.actualReceives);
        assertEquals(0, secondBattery.actualReceives);
    }

    @Test
    void respectsTheSourcesInitialExtractionLimit() {
        TestEnergyStorage source = TestEnergyStorage.source(1_000, 1);
        TestEnergyStorage sink = TestEnergyStorage.sink(1_000);

        int transferred = EnergyDistributor.distribute(List.of(
            endpoint(0, source),
            endpoint(1, sink)
        ), 1_000);

        assertEquals(1, transferred);
        assertEquals(999, source.energy);
        assertEquals(1, sink.energy);
        assertEquals(1, source.actualExtractions);
        assertEquals(1, sink.actualReceives);
    }

    @Test
    void capsRedistributionForRateLimitedSinks() {
        TestEnergyStorage source = TestEnergyStorage.source(1_000, 1_000);
        TestEnergyStorage sink = new TestEnergyStorage(0, 1_000, 1, false, true);

        int transferred = EnergyDistributor.distribute(List.of(
            endpoint(0, source),
            endpoint(1, sink)
        ), 1_000);

        assertEquals(3, transferred);
        assertEquals(3, source.actualExtractions);
        assertEquals(3, sink.actualReceives);
    }

    @Test
    void transfersTheAvailableBudgetBetweenDedicatedEndpoints() {
        TestEnergyStorage source = TestEnergyStorage.source(2_000, 2_000);
        TestEnergyStorage sink = TestEnergyStorage.sink(2_000);

        int transferred = EnergyDistributor.distribute(List.of(
            endpoint(0, source),
            endpoint(1, sink)
        ), 1_000);

        assertEquals(1_000, transferred);
        assertEquals(1_000, source.energy);
        assertEquals(1_000, sink.energy);
    }

    private static TestEndpoint endpoint(int identity, TestEnergyStorage storage) {
        return new TestEndpoint(identity, storage);
    }

    private record TestEndpoint(int id, TestEnergyStorage storage) implements EnergyDistributor.Endpoint {
        @Override
        public Object identity() {
            return id;
        }

        @Override
        public int receive(int amount, boolean simulate) {
            return storage.receiveEnergy(amount, simulate);
        }

        @Override
        public int extract(int amount, boolean simulate) {
            return storage.extractEnergy(amount, simulate);
        }

        @Override
        public boolean canExtract() {
            return storage.canExtract;
        }

        @Override
        public boolean canReceive() {
            return storage.canReceive;
        }
    }

    private static final class TestEnergyStorage {
        private int energy;
        private final int capacity;
        private final int operationLimit;
        private final boolean canExtract;
        private final boolean canReceive;
        private int actualExtractions;
        private int actualReceives;

        private TestEnergyStorage(int energy, int capacity, int operationLimit,
                                  boolean canExtract, boolean canReceive) {
            this.energy = energy;
            this.capacity = capacity;
            this.operationLimit = operationLimit;
            this.canExtract = canExtract;
            this.canReceive = canReceive;
        }

        private static TestEnergyStorage battery(int energy, int capacity, int operationLimit) {
            return new TestEnergyStorage(energy, capacity, operationLimit, true, true);
        }

        private static TestEnergyStorage source(int energy, int operationLimit) {
            return new TestEnergyStorage(energy, energy, operationLimit, true, false);
        }

        private static TestEnergyStorage sink(int capacity) {
            return new TestEnergyStorage(0, capacity, capacity, false, true);
        }

        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive || maxReceive <= 0) return 0;
            int accepted = Math.min(Math.min(maxReceive, operationLimit), capacity - energy);
            if (!simulate && accepted > 0) {
                energy += accepted;
                actualReceives++;
            }
            return accepted;
        }

        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!canExtract || maxExtract <= 0) return 0;
            int extracted = Math.min(Math.min(maxExtract, operationLimit), energy);
            if (!simulate && extracted > 0) {
                energy -= extracted;
                actualExtractions++;
            }
            return extracted;
        }
    }
}
