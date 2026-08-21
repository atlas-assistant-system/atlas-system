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

    @ParameterizedTest
    @CsvSource({"2136, 1922", "2000, 1800", "0, 0", "1, 1"})
    void shouldPlaceItsLowerBoundBelowTheTarget(int target, int expected) {
        assertThat(new Calories(target).lowerBound()).isEqualTo(new Calories(expected));
    }

    @ParameterizedTest
    @CsvSource({"2136, 2350", "2000, 2200", "0, 0", "1, 1"})
    void shouldPlaceItsUpperBoundAboveTheTarget(int target, int expected) {
        assertThat(new Calories(target).upperBound()).isEqualTo(new Calories(expected));
    }

    @ParameterizedTest
    @CsvSource({"1922, true", "2136, true", "2350, true", "1921, false", "2351, false", "0, false"})
    void shouldCoverOnlyWhatFallsInsideTheTolerance(int consumed, boolean expected) {
        assertThat(new Calories(2_136).covers(new Calories(consumed))).isEqualTo(expected);
    }

    @Test
    void shouldCoverNothingWithoutATarget() {
        assertThat(new Calories(0).covers(new Calories(0))).isFalse();
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(new Calories(1_940)).isEqualTo(new Calories(1_940));
    }
}
