package atlas.domain.nutrition.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

class DayTotalsTest {

    private static final Macros TARGET = new Macros(150, 200, 60);

    @Test
    void shouldReportWhatIsLeftOfEachMacro() {
        var totals = new DayTotals(new Macros(90, 120, 20), TARGET);

        assertThat(totals.remainingProtein()).isEqualTo(60);
        assertThat(totals.remainingCarbs()).isEqualTo(80);
        assertThat(totals.remainingFat()).isEqualTo(40);
    }

    @Test
    void shouldTellApartGoingOverOnOneMacroFromFallingShortOnAnother() {
        var totals = new DayTotals(new Macros(180, 120, 60), TARGET);

        assertThat(totals.remainingProtein()).isEqualTo(-30);
        assertThat(totals.remainingCarbs()).isEqualTo(80);
        assertThat(totals.remainingFat()).isZero();
    }

    @Test
    void shouldDeriveBothSidesOfTheCalorieBudgetFromTheMacros() {
        var totals = new DayTotals(new Macros(75, 100, 30), TARGET);

        assertThat(totals.consumedCalories()).isEqualTo(new Calories(970));
        assertThat(totals.targetCalories()).isEqualTo(new Calories(1_940));
        assertThat(totals.remainingCalories()).isEqualTo(970);
        assertThat(totals.caloriePercentage()).isEqualTo(50);
    }

    @Test
    void shouldBeOverBudgetOnlyWhenTheCaloriesAreExceeded() {
        assertThat(new DayTotals(TARGET, TARGET).isOverBudget()).isFalse();
        assertThat(new DayTotals(new Macros(150, 200, 61), TARGET).isOverBudget()).isTrue();
    }

    @Test
    void shouldReportAnEmptyDayAsTheWholeQuotaStillAvailable() {
        var totals = new DayTotals(new Macros(0, 0, 0), TARGET);

        assertThat(totals.remainingCalories()).isEqualTo(1_940);
        assertThat(totals.caloriePercentage()).isZero();
        assertThat(totals.isOverBudget()).isFalse();
    }

    @Test
    void shouldRejectTotalsWithoutAConsumedOrTargetSide() {
        assertThatThrownBy(() -> new DayTotals(null, TARGET)).isInstanceOf(GuardException.class);
        assertThatThrownBy(() -> new DayTotals(TARGET, null)).isInstanceOf(GuardException.class);
    }
}
