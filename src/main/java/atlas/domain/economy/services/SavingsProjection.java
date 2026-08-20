package atlas.domain.economy.services;

import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.Projection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public final class SavingsProjection {

    public Projection of(Money target, long netCents, int overMonths, LocalDate today, LocalDate deadline) {
        var monthlySaving = rate(netCents, overMonths);
        var monthsRemaining = monthsBetween(today, deadline);
        var projected = monthlySaving * monthsRemaining;

        return new Projection(
            target.cents(),
            monthlySaving,
            monthsRemaining,
            projected,
            requiredRate(target.cents(), monthsRemaining),
            projected >= target.cents());
    }

    private static long rate(long netCents, int overMonths) {
        return divide(netCents, overMonths, RoundingMode.HALF_UP);
    }

    private static long requiredRate(long targetCents, int monthsRemaining) {
        return monthsRemaining == 0 ? targetCents : divide(targetCents, monthsRemaining, RoundingMode.CEILING);
    }

    private static long divide(long value, int by, RoundingMode rounding) {
        return BigDecimal.valueOf(value)
            .divide(BigDecimal.valueOf(by), 0, rounding)
            .longValueExact();
    }

    private static int monthsBetween(LocalDate today, LocalDate deadline) {
        var months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline));

        return (int) Math.max(0, months);
    }
}
