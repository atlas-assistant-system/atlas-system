package atlas.domain.nutrition.enums;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.nutrition.vos.Weight;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class GoalTest {

    @ParameterizedTest
    @CsvSource({"84000, 78000, LOSE", "78000, 84000, GAIN", "78000, 78000, MAINTAIN"})
    void shouldDeriveTheGoalFromTheTwoWeights(int start, int target, Goal expected) {
        assertThat(Goal.of(weight(start), weight(target))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"78000, 78500", "78000, 77500", "78000, 78499"})
    void shouldReadHalfAKiloOfDifferenceAsMaintaining(int start, int target) {
        assertThat(Goal.of(weight(start), weight(target))).isEqualTo(Goal.MAINTAIN);
    }

    @ParameterizedTest
    @CsvSource({"78000, 78501, GAIN", "78000, 77499, LOSE"})
    void shouldLeaveMaintainingAsSoonAsTheToleranceIsPassed(int start, int target, Goal expected) {
        assertThat(Goal.of(weight(start), weight(target))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"78000, false", "77000, true", "76000, true"})
    void shouldReachALosingGoalOnlyAtOrBelowTheTarget(int current, boolean expected) {
        assertThat(Goal.LOSE.isReached(weight(current), weight(77_000))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"78000, false", "79000, true", "80000, true"})
    void shouldReachAGainingGoalOnlyAtOrAboveTheTarget(int current, boolean expected) {
        assertThat(Goal.GAIN.isReached(weight(current), weight(79_000))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"78000, true", "78500, true", "78501, false", "77499, false"})
    void shouldReachAMaintainingGoalWithinTheTolerance(int current, boolean expected) {
        assertThat(Goal.MAINTAIN.isReached(weight(current), weight(78_000))).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(Goal.class)
    void shouldCarryALabelForTheMirror(Goal goal) {
        assertThat(goal.label()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Goal.class)
    void shouldSurvivePersistenceByName(Goal goal) {
        assertThat(Goal.valueOf(goal.name())).isEqualTo(goal);
    }

    private static Weight weight(int grams) {
        return Weight.ofGrams(grams).value();
    }
}
