package atlas.domain.economy.services;

import atlas.domain.economy.enums.BudgetStatus;
import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.Pace;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class BudgetPace {

    public Pace of(Money limit, long spentCents, LocalDate today) {
        var projectedCents = project(spentCents, today);

        return new Pace(limit.cents(), spentCents, projectedCents, statusOf(limit.cents(), spentCents,
            projectedCents));
    }

    private static long project(long spentCents, LocalDate today) {
        return BigDecimal.valueOf(spentCents)
            .multiply(BigDecimal.valueOf(today.lengthOfMonth()))
            .divide(BigDecimal.valueOf(today.getDayOfMonth()), 0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    private static BudgetStatus statusOf(long limitCents, long spentCents, long projectedCents) {
        if (spentCents >= limitCents) {
            return BudgetStatus.EXCEEDED;
        }

        return projectedCents > limitCents ? BudgetStatus.AT_RISK : BudgetStatus.WITHIN;
    }
}
