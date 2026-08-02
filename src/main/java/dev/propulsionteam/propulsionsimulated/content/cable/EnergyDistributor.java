package dev.propulsionteam.propulsionsimulated.content.cable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Performs a bounded routing cycle without allowing energy received this tick to become a source. */
final class EnergyDistributor {
    private static final int MAX_DISTRIBUTION_PASSES = 3;

    private EnergyDistributor() {}

    static int distribute(List<? extends Endpoint> endpoints, int requestedBudget) {
        int budget = Math.max(0, requestedBudget);
        if (budget == 0 || endpoints.isEmpty()) {
            return 0;
        }

        List<Source> sources = new ArrayList<>();
        List<Endpoint> sinks = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            if (endpoint.canExtract()) {
                int availableAtStart = endpoint.extract(budget, true);
                if (availableAtStart > 0) {
                    sources.add(new Source(endpoint, availableAtStart));
                }
            }
            if (endpoint.canReceive()) {
                sinks.add(endpoint);
            }
        }
        if (sources.isEmpty() || sinks.isEmpty()) {
            return 0;
        }

        Set<Object> receivedThisTick = new HashSet<>();
        boolean progressed;
        int passes = 0;
        do {
            progressed = false;
            int activeSinks = 0;
            for (Endpoint sink : sinks) {
                if (sink.receive(Math.max(1, budget / Math.max(1, sinks.size())), true) > 0
                    && hasEligibleSource(sources, sink.identity(), receivedThisTick)) {
                    activeSinks++;
                }
            }
            if (activeSinks == 0) break;

            int share = Math.max(1, budget / activeSinks);
            int remainder = budget % activeSinks;
            for (Endpoint sink : sinks) {
                if (budget <= 0) break;
                int requested = Math.min(budget, share + (remainder-- > 0 ? 1 : 0));
                int acceptedSimulation = sink.receive(requested, true);
                if (acceptedSimulation <= 0) continue;
                int extracted = extract(sources, acceptedSimulation, sink.identity(), receivedThisTick);
                if (extracted <= 0) continue;
                int accepted = sink.receive(extracted, false);
                if (accepted > 0) {
                    budget -= accepted;
                    receivedThisTick.add(sink.identity());
                    progressed = true;
                }
            }
            passes++;
        } while (budget > 0 && progressed && passes < MAX_DISTRIBUTION_PASSES);

        return Math.max(0, requestedBudget) - budget;
    }

    private static boolean hasEligibleSource(List<Source> sources, Object sinkIdentity,
                                             Set<Object> receivedThisTick) {
        for (Source source : sources) {
            if (source.remaining > 0
                && !source.endpoint.identity().equals(sinkIdentity)
                && !receivedThisTick.contains(source.endpoint.identity())) {
                return true;
            }
        }
        return false;
    }

    private static int extract(List<Source> sources, int requested, Object sinkIdentity,
                               Set<Object> receivedThisTick) {
        int remaining = requested;
        for (Source source : sources) {
            if (remaining <= 0) break;
            Object sourceIdentity = source.endpoint.identity();
            if (source.remaining <= 0
                || sourceIdentity.equals(sinkIdentity)
                || receivedThisTick.contains(sourceIdentity)) {
                continue;
            }
            int available = source.endpoint.extract(Math.min(remaining, source.remaining), true);
            if (available > 0) {
                int extracted = source.endpoint.extract(Math.min(available, source.remaining), false);
                source.remaining -= extracted;
                remaining -= extracted;
            }
        }
        return requested - remaining;
    }

    interface Endpoint {
        Object identity();

        int receive(int amount, boolean simulate);

        int extract(int amount, boolean simulate);

        boolean canExtract();

        boolean canReceive();
    }

    private static final class Source {
        private final Endpoint endpoint;
        private int remaining;

        private Source(Endpoint endpoint, int remaining) {
            this.endpoint = endpoint;
            this.remaining = remaining;
        }
    }
}
