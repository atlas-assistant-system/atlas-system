package atlas.domain.nutrition.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.nutrition.NutritionErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class WeightTest {

    @ParameterizedTest
    @ValueSource(ints = {19_999, 400_001, 0, -78_400})
    void shouldFailWhenGramsAreOutsideWhatAPersonCanWeigh(int grams) {
        var result = Weight.ofGrams(grams);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(NutritionErrors.WEIGHT_OUT_OF_RANGE);
    }

    @ParameterizedTest
    @ValueSource(ints = {Weight.MIN_GRAMS, 78_400, Weight.MAX_GRAMS})
    void shouldAcceptGramsInsideTheRange(int grams) {
        assertThat(Weight.ofGrams(grams).value().grams()).isEqualTo(grams);
    }

    @ParameterizedTest
    @CsvSource({"78.4, 78400", "78, 78000", "78.437, 78437", "20, 20000"})
    void shouldConvertKilogramsToGrams(String kilograms, int expectedGrams) {
        assertThat(Weight.ofKilograms(new BigDecimal(kilograms)).value().grams()).isEqualTo(expectedGrams);
    }

    @ParameterizedTest
    @CsvSource({"78.4375, 78438", "78.4374, 78437", "78.0005, 78001"})
    void shouldRoundKilogramsToTheNearestGram(String kilograms, int expectedGrams) {
        assertThat(Weight.ofKilograms(new BigDecimal(kilograms)).value().grams()).isEqualTo(expectedGrams);
    }

    @Test
    void shouldFailWhenKilogramsAreMissing() {
        assertThat(Weight.ofKilograms(null).error()).isEqualTo(NutritionErrors.WEIGHT_OUT_OF_RANGE);
    }

    @Test
    void shouldFailWhenKilogramsDoNotFitInGrams() {
        var result = Weight.ofKilograms(new BigDecimal("99999999999"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(NutritionErrors.WEIGHT_OUT_OF_RANGE);
    }

    @Test
    void shouldExposeTheWeightInKilograms() {
        assertThat(Weight.ofGrams(78_400).value().toKilograms()).isEqualByComparingTo("78.4");
    }

    @Test
    void shouldMeasureTheGramsToLoseAsANegativeDifference() {
        var current = Weight.ofGrams(84_000).value();
        var target = Weight.ofGrams(78_000).value();

        assertThat(current.gramsTo(target)).isEqualTo(-6_000);
    }

    @Test
    void shouldMeasureTheGramsToGainAsAPositiveDifference() {
        var current = Weight.ofGrams(78_000).value();
        var target = Weight.ofGrams(84_000).value();

        assertThat(current.gramsTo(target)).isEqualTo(6_000);
    }

    @Test
    void shouldRejectAnOutOfRangeWeightBuiltDirectly() {
        assertThatThrownBy(() -> new Weight(1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(Weight.ofGrams(78_400).value()).isEqualTo(new Weight(78_400));
    }
}
