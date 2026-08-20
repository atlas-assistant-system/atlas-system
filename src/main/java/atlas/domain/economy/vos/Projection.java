package atlas.domain.economy.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;

public record Projection(
    long targetCents,
    long monthlySavingCents,
    int monthsRemaining,
    long projectedCents,
    long requiredMonthlyCents,
    boolean reachable) implements ValueObject {

    public Projection {
        NumberGuard.positive(targetCents, "targetCents");
        NumberGuard.notNegative(monthsRemaining, "monthsRemaining");
    }
}
