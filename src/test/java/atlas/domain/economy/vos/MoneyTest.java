package atlas.domain.economy.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.economy.MovementErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -1250})
    void shouldFailWhenCentsAreNotPositive(long cents) {
        var result = Money.ofCents(cents);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
    }

    @Test
    void shouldExposeTheAmountInEuros() {
        assertThat(Money.ofCents(1250).value().toEuros()).isEqualByComparingTo("12.50");
    }

    @ParameterizedTest
    @CsvSource({"12.50, 1250", "0.01, 1", "1, 100", "1234.56, 123456"})
    void shouldConvertEurosToCents(String euros, long expectedCents) {
        assertThat(Money.ofEuros(new BigDecimal(euros)).value().cents()).isEqualTo(expectedCents);
    }

    @ParameterizedTest
    @CsvSource({"12.005, 1201", "12.004, 1200", "0.005, 1"})
    void shouldRoundEurosToTheNearestCent(String euros, long expectedCents) {
        assertThat(Money.ofEuros(new BigDecimal(euros)).value().cents()).isEqualTo(expectedCents);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "0.004"})
    void shouldFailWhenEurosDoNotReachOneCent(String euros) {
        assertThat(Money.ofEuros(new BigDecimal(euros)).error())
            .isEqualTo(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
    }

    @Test
    void shouldFailWhenEurosAreMissing() {
        assertThat(Money.ofEuros(null).error()).isEqualTo(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
    }

    @Test
    void shouldAddAmountsOfTheSameCurrency() {
        var total = Money.ofCents(1250).value().plus(Money.ofCents(750).value());

        assertThat(total.cents()).isEqualTo(2000);
    }

    @Test
    void shouldSubtractAmountsOfTheSameCurrency() {
        var rest = Money.ofCents(2000).value().minus(Money.ofCents(750).value());

        assertThat(rest.cents()).isEqualTo(1250);
    }

    @Test
    void shouldRejectSubtractionThatWouldLeaveNothing() {
        var money = Money.ofCents(1000).value();

        assertThatThrownBy(() -> money.minus(money)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldRejectOperationsBetweenDifferentCurrencies() {
        var euros = Money.ofCents(1000).value();
        var dollars = new Money(1000, "USD");

        assertThatThrownBy(() -> euros.plus(dollars)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldDefaultToEuros() {
        assertThat(Money.ofCents(1).value().currency()).isEqualTo(Money.EUR);
    }

    @Test
    void shouldTreatAmountsOfTheSameCentsAndCurrencyAsEqual() {
        assertThat(Money.ofCents(1250).value()).isEqualTo(Money.ofCents(1250).value());
    }
}
