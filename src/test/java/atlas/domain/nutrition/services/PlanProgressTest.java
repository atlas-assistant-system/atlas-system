package atlas.domain.nutrition.services;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.PlanId;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInId;
import atlas.domain.nutrition.enums.Goal;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Weight;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlanProgressTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Instant NOW = Instant.parse("2026-08-22T07:00:00Z");

    private final PlanProgress progress = new PlanProgress();

    @Test
    void shouldFallBackToTheStartingWeightWhenNobodyHasWeighedIn() {
        var result = progress.of(losingPlan(), List.of(), TODAY);

        assertThat(result.current()).isEqualTo(new Weight(84_000));
        assertThat(result.remainingGrams()).isEqualTo(-6_000);
        assertThat(result.percentage()).isZero();
        assertThat(result.reached()).isFalse();
    }

    @Test
    void shouldTakeTheLatestReadingAsTheCurrentWeight() {
        var result = progress.of(losingPlan(), List.of(
            weighIn(1, 83_000, TODAY.minusDays(5)),
            weighIn(2, 81_000, TODAY.minusDays(1)),
            weighIn(3, 82_000, TODAY.minusDays(3))), TODAY);

        assertThat(result.current()).isEqualTo(new Weight(81_000));
    }

    @Test
    void shouldReportTheGramsLeftWithTheirSign() {
        var result = progress.of(losingPlan(), List.of(weighIn(1, 81_000, TODAY)), TODAY);

        assertThat(result.remainingGrams()).isEqualTo(-3_000);
        assertThat(result.goal()).isEqualTo(Goal.LOSE);
    }

    @ParameterizedTest
    @CsvSource({"84000, 0", "82500, 25", "81000, 50", "78000, 100"})
    void shouldMeasureHowMuchOfTheRoadIsBehind(int current, int expected) {
        var result = progress.of(losingPlan(), List.of(weighIn(1, current, TODAY)), TODAY);

        assertThat(result.percentage()).isEqualTo(expected);
    }

    @Test
    void shouldCapTheProgressAtAHundredWhenTheTargetIsPassed() {
        var result = progress.of(losingPlan(), List.of(weighIn(1, 74_000, TODAY)), TODAY);

        assertThat(result.percentage()).isEqualTo(100);
        assertThat(result.reached()).isTrue();
    }

    @Test
    void shouldFloorTheProgressAtZeroWhenGoingTheWrongWay() {
        var result = progress.of(losingPlan(), List.of(weighIn(1, 88_000, TODAY)), TODAY);

        assertThat(result.percentage()).isZero();
        assertThat(result.reached()).isFalse();
    }

    @Test
    void shouldMeasureAGainingPlanInTheOtherDirection() {
        var plan = planOf(78_000, 84_000);

        var result = progress.of(plan, List.of(weighIn(1, 81_000, TODAY)), TODAY);

        assertThat(result.goal()).isEqualTo(Goal.GAIN);
        assertThat(result.remainingGrams()).isEqualTo(3_000);
        assertThat(result.percentage()).isEqualTo(50);
    }

    @Test
    void shouldReportAMaintainingPlanAsReachedInsideTheTolerance() {
        var plan = planOf(78_000, 78_000);

        var result = progress.of(plan, List.of(weighIn(1, 78_400, TODAY)), TODAY);

        assertThat(result.goal()).isEqualTo(Goal.MAINTAIN);
        assertThat(result.reached()).isTrue();
        assertThat(result.percentage()).isEqualTo(100);
    }

    @Test
    void shouldReportAMaintainingPlanAsUnreachedOutsideTheTolerance() {
        var plan = planOf(78_000, 78_000);

        var result = progress.of(plan, List.of(weighIn(1, 80_000, TODAY)), TODAY);

        assertThat(result.reached()).isFalse();
        assertThat(result.percentage()).isZero();
    }

    @Test
    void shouldReportNoTrendWithoutTwoFullWeeksOfReadings() {
        var result = progress.of(losingPlan(), List.of(
            weighIn(1, 83_000, TODAY.minusDays(2)),
            weighIn(2, 82_500, TODAY)), TODAY);

        assertThat(result.trendGramsPerWeek()).isZero();
    }

    @Test
    void shouldMeasureTheTrendAsThisWeekAgainstTheOneBefore() {
        var result = progress.of(losingPlan(), List.of(
            weighIn(1, 84_000, TODAY.minusDays(13)),
            weighIn(2, 83_800, TODAY.minusDays(8)),
            weighIn(3, 83_000, TODAY.minusDays(5)),
            weighIn(4, 82_600, TODAY)), TODAY);

        assertThat(result.trendGramsPerWeek()).isEqualTo(-1_100);
    }

    @Test
    void shouldReportAFlatTrendWhenBothWeeksAverageTheSame() {
        var result = progress.of(losingPlan(), List.of(
            weighIn(1, 83_000, TODAY.minusDays(10)),
            weighIn(2, 83_000, TODAY.minusDays(3))), TODAY);

        assertThat(result.trendGramsPerWeek()).isZero();
    }

    @Test
    void shouldLeaveOutReadingsOlderThanTheTwoWindows() {
        var result = progress.of(losingPlan(), List.of(
            weighIn(1, 90_000, TODAY.minusDays(60)),
            weighIn(2, 83_000, TODAY.minusDays(10)),
            weighIn(3, 82_000, TODAY.minusDays(3))), TODAY);

        assertThat(result.trendGramsPerWeek()).isEqualTo(-1_000);
    }

    private static Plan losingPlan() {
        return planOf(84_000, 78_000);
    }

    private static Plan planOf(int startGrams, int targetGrams) {
        return Plan.rehydrate(
            PlanId.of(1),
            new Weight(startGrams),
            new Weight(targetGrams),
            new Macros(150, 200, 60),
            PlanStatus.ACTIVE,
            TODAY.minusDays(30),
            NOW);
    }

    private static WeighIn weighIn(int id, int grams, LocalDate day) {
        return WeighIn.rehydrate(WeighInId.of(id), new Weight(grams), day, NOW);
    }
}
