package atlas.domain.nutrition.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CaloriesTest {

    @Test
    void shouldRejectNegativeCalories() {
        assertThatThrownBy(() -> new Calories(-1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldReportWhatIsLeftToReachTheTarget() {
        assertThat(new Calories(1_600).remainingFor(new Calories(2_000))).isEqualTo(400);
    }

    @Test
    void shouldReportANegativeRemainderWhenTheTargetIsExceeded() {
        assertThat(new Calories(2_300).remainingFor(new Calories(2_000))).isEqualTo(-300);
    }

    @ParameterizedTest
    @CsvSource({"0, 2000, 0", "1000, 2000, 50", "2000, 2000, 100", "2300, 2000, 115", "1, 3, 33"})
    void shouldExpressItselfAsAPercentageOfTheTarget(int kcal, int total, int expected) {
        assertThat(new Calories(kcal).percentageOf(new Calories(total))).isEqualTo(expected);
    }

    @Test
    void shouldBeZeroPercentWhenThereIsNoTargetToCompareAgainst() {
        assertThat(new Calories(1_800).percentageOf(new Calories(0))).isZero();
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(new Calories(1_940)).isEqualTo(new Calories(1_940));
    }
}
