package atlas.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.nutrition.enums.Goal;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.domain.nutrition.events.PlanAdjustedEvent;
import atlas.domain.nutrition.events.PlanArchivedEvent;
import atlas.domain.nutrition.events.PlanDefinedEvent;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Weight;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlanTest {

    private static final PlanId ID = PlanId.of(1);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Instant NOW = Instant.parse("2026-08-22T07:30:00Z");
    private static final Calories DAILY_CALORIES = new Calories(1_940);
    private static final Macros DAILY_MACROS = new Macros(150, 200, 60);
    private static final Weight START = new Weight(84_000);
    private static final Weight TARGET = new Weight(78_000);

    @Test
    void shouldDefineAnActivePlanAndRaiseItsEvent() {
        var result = define(START, TARGET, DAILY_CALORIES, DAILY_MACROS, TODAY);

        assertThat(result.isSuccess()).isTrue();

        var plan = result.value();
        assertThat(plan.status()).isEqualTo(PlanStatus.ACTIVE);
        assertThat(plan.isArchived()).isFalse();
        assertThat(plan.startWeight()).isEqualTo(START);
        assertThat(plan.definedAt()).isEqualTo(NOW);
        assertThat(plan.pendingEvents()).containsExactly(new PlanDefinedEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenTheDailyQuotaIsEmpty() {
        var result = define(START, TARGET, new Calories(0), DAILY_MACROS, TODAY);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PlanErrors.CALORIES_REQUIRED);
    }

    @Test
    void shouldFailWhenItStartsInTheFuture() {
        var result = define(START, TARGET, DAILY_CALORIES, DAILY_MACROS, TODAY.plusDays(1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PlanErrors.CANNOT_START_IN_THE_FUTURE);
    }

    @Test
    void shouldAcceptAPlanThatStartedEarlier() {
        assertThat(define(START, TARGET, DAILY_CALORIES, DAILY_MACROS, TODAY.minusDays(30)).isSuccess()).isTrue();
    }

    @Test
    void shouldDeriveTheGoalFromTheTwoWeights() {
        assertThat(activePlan().goal()).isEqualTo(Goal.LOSE);
        assertThat(define(TARGET, START, DAILY_CALORIES, DAILY_MACROS, TODAY).value().goal()).isEqualTo(Goal.GAIN);
    }

    @Test
    void shouldKeepTheDailyCaloriesItWasGiven() {
        assertThat(activePlan().dailyCalories()).isEqualTo(DAILY_CALORIES);
    }

    @Test
    void shouldAdjustTheQuotaAndTheTargetAndRaiseItsEvent() {
        var plan = activePlan();
        var newTarget = new Weight(76_000);
        var newMacros = new Macros(160, 180, 55);

        var result = plan.adjust(new Calories(1_855), newMacros, newTarget, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(plan.dailyMacros()).isEqualTo(newMacros);
        assertThat(plan.targetWeight()).isEqualTo(newTarget);
        assertThat(plan.pendingEvents()).contains(new PlanAdjustedEvent(ID, NOW));
    }

    @Test
    void shouldKeepTheStartingWeightWhenTheTargetChanges() {
        var plan = activePlan();

        plan.adjust(DAILY_CALORIES, DAILY_MACROS, new Weight(76_000), NOW);

        assertThat(plan.startWeight()).isEqualTo(START);
    }

    @Test
    void shouldFailWhenTheAdjustedQuotaIsEmpty() {
        var plan = activePlan();

        var result = plan.adjust(new Calories(0), DAILY_MACROS, TARGET, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PlanErrors.CALORIES_REQUIRED);
        assertThat(plan.dailyMacros()).isEqualTo(DAILY_MACROS);
    }

    @Test
    void shouldArchiveThePlanAndRaiseItsEvent() {
        var plan = activePlan();

        var result = plan.archive(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(plan.isArchived()).isTrue();
        assertThat(plan.status()).isEqualTo(PlanStatus.ARCHIVED);
        assertThat(plan.pendingEvents()).contains(new PlanArchivedEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenArchivingATwiceArchivedPlan() {
        var plan = activePlan();
        plan.archive(NOW);

        var result = plan.archive(NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PlanErrors.ALREADY_ARCHIVED);
    }

    @Test
    void shouldFailWhenAdjustingAnArchivedPlan() {
        var plan = activePlan();
        plan.archive(NOW);

        var result = plan.adjust(new Calories(1_855), new Macros(160, 180, 55), TARGET, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PlanErrors.ALREADY_ARCHIVED);
        assertThat(plan.dailyMacros()).isEqualTo(DAILY_MACROS);
    }

    @Test
    void shouldRehydrateWithoutRaisingEvents() {
        var plan = Plan.rehydrate(
            ID, START, TARGET, DAILY_CALORIES, DAILY_MACROS, PlanStatus.ARCHIVED, TODAY, NOW);

        assertThat(plan.isArchived()).isTrue();
        assertThat(plan.startedOn()).isEqualTo(TODAY);
        assertThat(plan.pendingEvents()).isEmpty();
    }

    private static Plan activePlan() {
        return define(START, TARGET, DAILY_CALORIES, DAILY_MACROS, TODAY).value();
    }

    private static Result<Plan> define(
        Weight start, Weight target, Calories calories, Macros macros, LocalDate startedOn) {

        return Plan.define(ID, start, target, calories, macros, startedOn, TODAY, NOW);
    }
}
