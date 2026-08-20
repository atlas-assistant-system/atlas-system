package atlas.domain.economy.services;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.economy.enums.BudgetStatus;
import atlas.domain.economy.vos.Money;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BudgetPaceTest {

    private static final Money TWO_HUNDRED = Money.ofCents(20000).value();
    private static final LocalDate AUGUST_10 = LocalDate.of(2026, 8, 10);
    private static final LocalDate AUGUST_31 = LocalDate.of(2026, 8, 31);

    private final BudgetPace pace = new BudgetPace();

    @Test
    void shouldProjectTheMonthEndFromTheRateSoFar() {
        var projection = pace.of(TWO_HUNDRED, 18000, AUGUST_10);

        assertThat(projection.limitCents()).isEqualTo(20000);
        assertThat(projection.spentCents()).isEqualTo(18000);
        assertThat(projection.projectedCents()).isEqualTo(55800);
    }

    @Test
    void shouldWarnWhenTheRateWouldBlowTheLimit() {
        assertThat(pace.of(TWO_HUNDRED, 18000, AUGUST_10).status()).isEqualTo(BudgetStatus.AT_RISK);
    }

    @Test
    void shouldStayWithinWhenTheRateKeepsItUnderTheLimit() {
        var projection = pace.of(TWO_HUNDRED, 5000, AUGUST_10);

        assertThat(projection.projectedCents()).isEqualTo(15500);
        assertThat(projection.status()).isEqualTo(BudgetStatus.WITHIN);
    }

    @Test
    void shouldReportExceededOnceTheLimitIsSpent() {
        assertThat(pace.of(TWO_HUNDRED, 20000, AUGUST_10).status()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(pace.of(TWO_HUNDRED, 25000, AUGUST_10).status()).isEqualTo(BudgetStatus.EXCEEDED);
    }

    @Test
    void shouldStayWithinWhenTheProjectionLandsExactlyOnTheLimit() {
        var projection = pace.of(Money.ofCents(31000).value(), 10000, AUGUST_10);

        assertThat(projection.projectedCents()).isEqualTo(31000);
        assertThat(projection.status()).isEqualTo(BudgetStatus.WITHIN);
    }

    @Test
    void shouldProjectExactlyWhatWasSpentOnTheLastDayOfTheMonth() {
        var projection = pace.of(TWO_HUNDRED, 15000, AUGUST_31);

        assertThat(projection.projectedCents()).isEqualTo(15000);
        assertThat(projection.status()).isEqualTo(BudgetStatus.WITHIN);
    }

    @Test
    void shouldProjectNothingWhenNothingWasSpent() {
        var projection = pace.of(TWO_HUNDRED, 0, AUGUST_10);

        assertThat(projection.projectedCents()).isZero();
        assertThat(projection.status()).isEqualTo(BudgetStatus.WITHIN);
    }

    @ParameterizedTest
    @CsvSource({"2026-02-01, 1000, 28000", "2026-02-28, 1000, 1000", "2026-01-15, 1000, 2067"})
    void shouldUseTheRealLengthOfEachMonth(String today, long spent, long expected) {
        assertThat(pace.of(TWO_HUNDRED, spent, LocalDate.parse(today)).projectedCents()).isEqualTo(expected);
    }

    @Test
    void shouldRoundTheProjectionToTheNearestCent() {
        assertThat(pace.of(TWO_HUNDRED, 1000, LocalDate.of(2026, 8, 3)).projectedCents()).isEqualTo(10333);
    }
}
