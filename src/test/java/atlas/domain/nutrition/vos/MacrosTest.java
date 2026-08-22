package atlas.domain.nutrition.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.nutrition.NutritionErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MacrosTest {

    @ParameterizedTest
    @CsvSource({"-1, 0, 0", "0, -1, 0", "0, 0, -1", "-10, -10, -10"})
    void shouldFailWhenAnyMacroIsNegative(int protein, int carbs, int fat) {
        var result = Macros.create(protein, carbs, fat);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(NutritionErrors.MACROS_MUST_NOT_BE_NEGATIVE);
    }

    @Test
    void shouldAcceptAllMacrosAtZeroBecauseADayWithoutIntakesConsumesNothing() {
        var result = Macros.create(0, 0, 0);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().isZero()).isTrue();
    }

    @Test
    void shouldAddEachMacroSeparately() {
        var breakfast = Macros.create(30, 60, 10).value();
        var lunch = Macros.create(45, 80, 25).value();

        assertThat(breakfast.plus(lunch)).isEqualTo(new Macros(75, 140, 35));
    }

    @Test
    void shouldNotBeZeroWhenAnyMacroIsPresent() {
        assertThat(Macros.create(0, 0, 1).value().isZero()).isFalse();
    }

    @Test
    void shouldRejectANegativeMacroBuiltDirectly() {
        assertThatThrownBy(() -> new Macros(0, -1, 0)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(Macros.create(150, 200, 60).value()).isEqualTo(new Macros(150, 200, 60));
    }
}
