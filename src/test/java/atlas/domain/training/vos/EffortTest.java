package atlas.domain.training.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.training.TrainingErrors;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EffortTest {

    @ParameterizedTest
    @CsvSource({"-1, 0, 0, 0", "0, -1, 0, 0", "0, 0, -1, 0", "0, 0, 0, -1"})
    void shouldFailWhenAnyMeasureIsNegative(int load, int reps, int seconds, int meters) {
        var result = Effort.create(load, reps, seconds, meters);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(TrainingErrors.MEASURES_MUST_NOT_BE_NEGATIVE);
    }

    @Test
    void shouldRejectALoadNoOneCouldLiftBecauseItIsATypedComma() {
        var result = Effort.create(725_000, 8, 0, 0);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(TrainingErrors.LOAD_OUT_OF_RANGE);
    }

    @Test
    void shouldAcceptTheHeaviestBelievableLoad() {
        assertThat(Effort.create(Effort.MAX_LOAD_GRAMS, 1, 0, 0).isSuccess()).isTrue();
    }

    @Test
    void shouldHoldAPressOfSeventyKilosForEightReps() {
        var effort = Effort.create(70_000, 8, 0, 0).value();

        assertThat(effort.loadGrams()).isEqualTo(70_000);
        assertThat(effort.reps()).isEqualTo(8);
        assertThat(effort.seconds()).isZero();
        assertThat(effort.meters()).isZero();
    }

    @Test
    void shouldBeZeroOnlyWhenTheFourMeasuresAreZero() {
        assertThat(Effort.NONE.isZero()).isTrue();
        assertThat(Effort.create(0, 12, 0, 0).value().isZero()).isFalse();
        assertThat(Effort.create(0, 0, 45, 0).value().isZero()).isFalse();
        assertThat(Effort.create(0, 0, 0, 5_000).value().isZero()).isFalse();
        assertThat(Effort.create(20_000, 0, 0, 0).value().isZero()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"72.5, 72500", "70, 70000", "2.25, 2250", "0, 0"})
    void shouldConvertKilogramsToGrams(String kilograms, int expected) {
        var effort = Effort.ofKilograms(new BigDecimal(kilograms), 8, 0, 0).value();

        assertThat(effort.loadGrams()).isEqualTo(expected);
    }

    @Test
    void shouldRoundKilogramsToTheNearestGram() {
        assertThat(Effort.ofKilograms(new BigDecimal("72.5004"), 1, 0, 0).value().loadGrams())
            .isEqualTo(72_500);
    }

    @Test
    void shouldFailWhenTheKilogramsAreNotThere() {
        var result = Effort.ofKilograms(null, 8, 0, 0);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(TrainingErrors.LOAD_OUT_OF_RANGE);
    }

    @Test
    void shouldFailWhenTheKilogramsAreTooBigToBeAnInteger() {
        var result = Effort.ofKilograms(new BigDecimal("999999999999"), 1, 0, 0);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(TrainingErrors.LOAD_OUT_OF_RANGE);
    }

    @Test
    void shouldAcceptExactlyTheHeaviestBelievableLoadInKilograms() {
        var result = Effort.ofKilograms(new BigDecimal("500"), 1, 0, 0);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().loadGrams()).isEqualTo(Effort.MAX_LOAD_GRAMS);
    }

    @Test
    void shouldRejectOneGramOverTheHeaviestBelievableLoad() {
        assertThat(Effort.ofKilograms(new BigDecimal("500.001"), 1, 0, 0).isFailure()).isTrue();
    }

    @Test
    void shouldGiveTheLoadBackInKilograms() {
        assertThat(new Effort(72_500, 8, 0, 0).loadKilograms()).isEqualByComparingTo("72.5");
        assertThat(Effort.NONE.loadKilograms()).isEqualByComparingTo("0");
    }

    @Test
    void shouldRejectANegativeMeasureBuiltDirectly() {
        assertThatThrownBy(() -> new Effort(0, -1, 0, 0)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(Effort.create(70_000, 8, 0, 0).value()).isEqualTo(new Effort(70_000, 8, 0, 0));
    }
}
