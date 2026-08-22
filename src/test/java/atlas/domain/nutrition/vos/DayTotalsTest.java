package atlas.domain.nutrition.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

class DayTotalsTest {

    private static final Macros TARGET_MACROS = new Macros(150, 200, 60);
    private static final Calories TARGET_CALORIES = new Calories(2_000);

    @Test
    void shouldReportWhatIsLeftOfEachMacro() {
        var totals = totalsOf(new Macros(90, 120, 20), new Calories(1_000));

        assertThat(totals.remainingProtein()).isEqualTo(60);
        assertThat(totals.remainingCarbs()).isEqualTo(80);
        assertThat(totals.remainingFat()).isEqualTo(40);
    }

    @Test
    void shouldTellApartGoingOverOnOneMacroFromFallingShortOnAnother() {
        var totals = totalsOf(new Macros(180, 120, 60), new Calories(1_000));

        assertThat(totals.remainingProtein()).isEqualTo(-30);
        assertThat(totals.remainingCarbs()).isEqualTo(80);
        assertThat(totals.remainingFat()).isZero();
    }

    @Test
    void shouldTakeTheCaloriesItWasGivenInsteadOfDerivingThem() {
        var totals = totalsOf(new Macros(75, 100, 30), new Calories(1_000));

        assertThat(totals.consumedCalories()).isEqualTo(new Calories(1_000));
        assertThat(totals.targetCalories()).isEqualTo(TARGET_CALORIES);
        assertThat(totals.remainingCalories()).isEqualTo(1_000);
        assertThat(totals.caloriePercentage()).isEqualTo(50);
    }

    @Test
    void shouldReportTheToleranceRangeAroundTheTarget() {
        var totals = totalsOf(new Macros(75, 100, 30), new Calories(1_000));

        assertThat(totals.lowerTarget()).isEqualTo(new Calories(1_800));
        assertThat(totals.upperTarget()).isEqualTo(new Calories(2_200));
        assertThat(totals.isWithinRange()).isFalse();
    }

    @Test
    void shouldBeWithinRangeCloseToTheTarget() {
        assertThat(totalsOf(TARGET_MACROS, new Calories(1_950)).isWithinRange()).isTrue();
    }

    @Test
    void shouldBeOverBudgetOnlyWhenTheCaloriesAreExceeded() {
        assertThat(totalsOf(TARGET_MACROS, TARGET_CALORIES).isOverBudget()).isFalse();
        assertThat(totalsOf(TARGET_MACROS, new Calories(2_001)).isOverBudget()).isTrue();
    }

    @Test
    void shouldReportAnEmptyDayAsTheWholeQuotaStillAvailable() {
        var totals = totalsOf(Macros.NONE, Calories.NONE);

        assertThat(totals.remainingCalories()).isEqualTo(2_000);
        assertThat(totals.caloriePercentage()).isZero();
        assertThat(totals.isOverBudget()).isFalse();
    }

    @Test
    void shouldRejectTotalsWithAMissingSide() {
        assertThatThrownBy(() -> new DayTotals(null, Calories.NONE, TARGET_MACROS, TARGET_CALORIES))
            .isInstanceOf(GuardException.class);
        assertThatThrownBy(() -> new DayTotals(Macros.NONE, null, TARGET_MACROS, TARGET_CALORIES))
            .isInstanceOf(GuardException.class);
        assertThatThrownBy(() -> new DayTotals(Macros.NONE, Calories.NONE, null, TARGET_CALORIES))
            .isInstanceOf(GuardException.class);
        assertThatThrownBy(() -> new DayTotals(Macros.NONE, Calories.NONE, TARGET_MACROS, null))
            .isInstanceOf(GuardException.class);
    }

    private static DayTotals totalsOf(Macros consumedMacros, Calories consumedCalories) {
        return new DayTotals(consumedMacros, consumedCalories, TARGET_MACROS, TARGET_CALORIES);
    }
}
