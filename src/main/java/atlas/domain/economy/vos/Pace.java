package atlas.domain.economy.vos;

import atlas.domain.economy.enums.BudgetStatus;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;

public record Pace(long limitCents, long spentCents, long projectedCents, BudgetStatus status)
    implements ValueObject {

    public Pace {
        NumberGuard.positive(limitCents, "limitCents");
        NumberGuard.notNegative(spentCents, "spentCents");
        NumberGuard.notNegative(projectedCents, "projectedCents");
    }
}
