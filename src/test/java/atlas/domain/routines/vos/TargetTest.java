package atlas.domain.routines.vos;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.routines.RoutineErrors;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TargetTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.5", "0.00"})
    void shouldFailWhenNotPositive(String amount) {
        var result = Target.create(new BigDecimal(amount));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(RoutineErrors.TARGET_MUST_BE_POSITIVE);
    }

    @Test
    void shouldFailWhenAmountIsMissing() {
        assertThat(Target.create(null).error()).isEqualTo(RoutineErrors.TARGET_MUST_BE_POSITIVE);
    }

    @Test
    void shouldAcceptFractionalAmounts() {
        var target = Target.create(new BigDecimal("1.5"), Unit.create("L").value().orElseThrow()).value();

        assertThat(target.amount()).isEqualByComparingTo("1.5");
        assertThat(target.unit()).map(Unit::value).contains("L");
    }

    @Test
    void shouldLeaveUnitEmptyWhenNoneIsGiven() {
        assertThat(Target.create(BigDecimal.ONE).value().unit()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"2, 2.0", "2, 2.000", "100, 1E+2", "0.5, 0.50"})
    void shouldTreatAmountsOfDifferentScaleAsTheSameTarget(String one, String other) {
        assertThat(Target.create(new BigDecimal(one))
            .value()).isEqualTo(Target.create(new BigDecimal(other)).value());
    }

    @Test
    void shouldKeepRoundAmountsReadable() {
        assertThat(Target.create(new BigDecimal("100")).value().amount()).hasToString("100");
    }

    @ParameterizedTest
    @CsvSource({
        "2, 1, false",
        "2, 2, true",
        "2, 3, true", // se puede marcar de mas: 3/2 se registra tal cual
        "1.5, 1.50, true",
        "1.5, 1.4999, false"})
    void shouldTellWhetherTheQuotaIsCovered(String target, String logged, boolean expected) {
        assertThat(Target.create(new BigDecimal(target)).value().isMetBy(new BigDecimal(logged)))
            .isEqualTo(expected);
    }
}
