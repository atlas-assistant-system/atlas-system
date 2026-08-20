package atlas.domain.economy.services;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.economy.vos.Money;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SavingsProjectionTest {

    private static final Money THREE_THOUSAND = Money.ofCents(300000).value();
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final LocalDate IN_SIX_MONTHS = LocalDate.of(2027, 2, 28);

    private final SavingsProjection projection = new SavingsProjection();

    @Test
    void shouldTurnThePeriodNetIntoAMonthlyRate() {
        var forecast = projection.of(THREE_THOUSAND, 600000, 6, TODAY, IN_SIX_MONTHS);

        assertThat(forecast.monthlySavingCents()).isEqualTo(100000);
        assertThat(forecast.monthsRemaining()).isEqualTo(6);
        assertThat(forecast.projectedCents()).isEqualTo(600000);
    }

    @Test
    void shouldSayItIsReachableWhenTheRateGetsThere() {
        assertThat(projection.of(THREE_THOUSAND, 600000, 6, TODAY, IN_SIX_MONTHS).reachable()).isTrue();
    }

    @Test
    void shouldSayItIsNotReachableWhenTheRateFallsShort() {
        var forecast = projection.of(THREE_THOUSAND, 120000, 6, TODAY, IN_SIX_MONTHS);

        assertThat(forecast.projectedCents()).isEqualTo(120000);
        assertThat(forecast.reachable()).isFalse();
    }

    @Test
    void shouldSayWhatTheRateWouldHaveToBe() {
        var forecast = projection.of(THREE_THOUSAND, 120000, 6, TODAY, IN_SIX_MONTHS);

        assertThat(forecast.requiredMonthlyCents()).isEqualTo(50000);
    }

    @Test
    void shouldRoundTheRequiredRateUpSoItAlwaysGetsThere() {
        var forecast = projection.of(Money.ofCents(100000).value(), 0, 6, TODAY, IN_SIX_MONTHS);

        assertThat(forecast.requiredMonthlyCents()).isEqualTo(16667);
    }

    @Test
    void shouldReportANegativeRateWhenSpendingBeatsIncome() {
        var forecast = projection.of(THREE_THOUSAND, -60000, 6, TODAY, IN_SIX_MONTHS);

        assertThat(forecast.monthlySavingCents()).isEqualTo(-10000);
        assertThat(forecast.projectedCents()).isEqualTo(-60000);
        assertThat(forecast.reachable()).isFalse();
    }

    @Test
    void shouldReachNothingWhenTheDeadlineIsInsideThisMonth() {
        var forecast = projection.of(THREE_THOUSAND, 600000, 6, TODAY, LocalDate.of(2026, 8, 31));

        assertThat(forecast.monthsRemaining()).isZero();
        assertThat(forecast.projectedCents()).isZero();
        assertThat(forecast.requiredMonthlyCents()).isEqualTo(300000);
        assertThat(forecast.reachable()).isFalse();
    }

    @Test
    void shouldNotCountMonthsBackwardsWhenTheDeadlineHasPassed() {
        var forecast = projection.of(THREE_THOUSAND, 600000, 6, TODAY, LocalDate.of(2026, 1, 31));

        assertThat(forecast.monthsRemaining()).isZero();
        assertThat(forecast.projectedCents()).isZero();
    }

    @Test
    void shouldReachItExactlyOnTheLimit() {
        var forecast = projection.of(THREE_THOUSAND, 300000, 6, TODAY, IN_SIX_MONTHS);

        assertThat(forecast.projectedCents()).isEqualTo(300000);
        assertThat(forecast.reachable()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"100000, 16667", "-100000, -16667", "60000, 10000", "1, 0"})
    void shouldRoundTheMonthlyRateToTheNearestCent(long netCents, long expectedRate) {
        assertThat(projection.of(THREE_THOUSAND, netCents, 6, TODAY, IN_SIX_MONTHS).monthlySavingCents())
            .isEqualTo(expectedRate);
    }
}
