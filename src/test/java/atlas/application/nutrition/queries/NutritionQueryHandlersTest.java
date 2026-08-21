package atlas.application.nutrition.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.nutrition.ports.DayConsumption;
import atlas.application.nutrition.ports.IntakeReadModel;
import atlas.application.nutrition.ports.PlanReadModel;
import atlas.application.nutrition.queries.getactiveplan.GetActivePlanQuery;
import atlas.application.nutrition.queries.getactiveplan.GetActivePlanQueryHandler;
import atlas.application.nutrition.queries.getday.GetDayQuery;
import atlas.application.nutrition.queries.getday.GetDayQueryHandler;
import atlas.application.nutrition.queries.getintake.GetIntakeQuery;
import atlas.application.nutrition.queries.getintake.GetIntakeQueryHandler;
import atlas.application.nutrition.queries.listdays.ListDaysQuery;
import atlas.application.nutrition.queries.listdays.ListDaysQueryHandler;
import atlas.application.nutrition.queries.listintakes.ListIntakesQuery;
import atlas.application.nutrition.queries.listintakes.ListIntakesQueryHandler;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.IntakeErrors;
import atlas.domain.nutrition.IntakeId;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.PlanErrors;
import atlas.domain.nutrition.PlanId;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Weight;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NutritionQueryHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T20:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final IntakeId INTAKE_ID = IntakeId.of(7);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final IntakeReadModel intakes = mock(IntakeReadModel.class);
    private final PlanReadModel plans = mock(PlanReadModel.class);

    private final GetActivePlanQueryHandler activePlan = new GetActivePlanQueryHandler(plans);
    private final GetDayQueryHandler day = new GetDayQueryHandler(intakes, plans, CLOCK);
    private final GetIntakeQueryHandler intake = new GetIntakeQueryHandler(intakes);
    private final ListIntakesQueryHandler listIntakes = new ListIntakesQueryHandler(intakes, CLOCK);
    private final ListDaysQueryHandler listDays = new ListDaysQueryHandler(intakes, plans, CLOCK);

    @Test
    void shouldReturnTheActivePlan() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));

        var result = activePlan.handle(new GetActivePlanQuery());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("N00000001");
        assertThat(result.value().goal()).isEqualTo("LOSE");
    }

    @Test
    void shouldFailWhenThereIsNoActivePlan() {
        when(plans.findActive()).thenReturn(Optional.empty());

        assertThat(activePlan.handle(new GetActivePlanQuery()).error()).isEqualTo(PlanErrors.NONE_ACTIVE);
    }

    @Test
    void shouldAddUpTheDayAgainstTheQuota() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(intakes.findOn(TODAY)).thenReturn(List.of(anIntakeOf(30, 60, 10), anIntakeOf(45, 40, 20)));

        var result = day.handle(new GetDayQuery(TODAY));

        assertThat(result.isSuccess()).isTrue();

        var totals = result.value();
        assertThat(totals.consumed().protein()).isEqualTo(75);
        assertThat(totals.consumed().carbs()).isEqualTo(100);
        assertThat(totals.consumed().fat()).isEqualTo(30);
        assertThat(totals.consumed().calories()).isEqualTo(970);
        assertThat(totals.target().calories()).isEqualTo(1_940);
        assertThat(totals.remaining().protein()).isEqualTo(75);
        assertThat(totals.remaining().calories()).isEqualTo(970);
        assertThat(totals.caloriePercentage()).isEqualTo(50);
        assertThat(totals.overBudget()).isFalse();
        assertThat(totals.intakes()).hasSize(2);
    }

    @Test
    void shouldReportTheDayWithoutAQuotaWhenThereIsNoPlan() {
        when(plans.findActive()).thenReturn(Optional.empty());
        when(intakes.findOn(TODAY)).thenReturn(List.of(anIntakeOf(30, 60, 10)));

        var result = day.handle(new GetDayQuery(TODAY));

        assertThat(result.value().consumed().calories()).isEqualTo(450);
        assertThat(result.value().target()).isNull();
        assertThat(result.value().remaining()).isNull();
        assertThat(result.value().caloriePercentage()).isZero();
        assertThat(result.value().overBudget()).isFalse();
    }

    @Test
    void shouldReportAnEmptyDayAsTheWholeQuotaAvailable() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(intakes.findOn(TODAY)).thenReturn(List.of());

        var result = day.handle(new GetDayQuery(TODAY));

        assertThat(result.value().consumed().calories()).isZero();
        assertThat(result.value().remaining().calories()).isEqualTo(1_940);
        assertThat(result.value().overBudget()).isFalse();
    }

    @Test
    void shouldFlagTheDayAsOverBudget() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(intakes.findOn(TODAY)).thenReturn(List.of(anIntakeOf(200, 250, 80)));

        var result = day.handle(new GetDayQuery(TODAY));

        assertThat(result.value().overBudget()).isTrue();
        assertThat(result.value().remaining().protein()).isEqualTo(-50);
    }

    @Test
    void shouldAskForTodayWhenNoDateIsGiven() {
        when(plans.findActive()).thenReturn(Optional.empty());
        when(intakes.findOn(TODAY)).thenReturn(List.of());

        assertThat(day.handle(new GetDayQuery(null)).value().date()).isEqualTo(TODAY);

        verify(intakes).findOn(TODAY);
    }

    @Test
    void shouldReturnTheIntake() {
        when(intakes.find(INTAKE_ID)).thenReturn(Optional.of(anIntakeOf(30, 60, 10)));

        assertThat(intake.handle(new GetIntakeQuery(INTAKE_ID)).value().id()).isEqualTo("I00000007");
    }

    @Test
    void shouldFailWhenTheIntakeIsNotThere() {
        when(intakes.find(INTAKE_ID)).thenReturn(Optional.empty());

        assertThat(intake.handle(new GetIntakeQuery(INTAKE_ID)).error())
            .isEqualTo(IntakeErrors.notFound(INTAKE_ID));
    }

    @Test
    void shouldListTheLastSevenDaysOfIntakesByDefault() {
        when(intakes.findBetween(any(), any(), anyInt()))
            .thenReturn(List.of(anIntakeOf(30, 60, 10)));

        var result = listIntakes.handle(new ListIntakesQuery(null, null, null));

        assertThat(result.value()).hasSize(1);

        var from = ArgumentCaptor.forClass(LocalDate.class);
        var to = ArgumentCaptor.forClass(LocalDate.class);
        var limit = ArgumentCaptor.forClass(Integer.class);
        verify(intakes).findBetween(from.capture(), to.capture(), limit.capture());

        assertThat(from.getValue()).isEqualTo(TODAY.minusDays(6));
        assertThat(to.getValue()).isEqualTo(TODAY);
        assertThat(limit.getValue()).isEqualTo(ListIntakesQueryHandler.DEFAULT_LIMIT);
    }

    @Test
    void shouldCapTheRequestedLimit() {
        listIntakes.handle(new ListIntakesQuery(TODAY, TODAY, 10_000));

        var limit = ArgumentCaptor.forClass(Integer.class);
        verify(intakes).findBetween(any(), any(), limit.capture());

        assertThat(limit.getValue()).isEqualTo(ListIntakesQueryHandler.MAX_LIMIT);
    }

    @Test
    void shouldSummariseEachDayAgainstTheQuotaNewestFirst() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(intakes.consumptionBetween(any(), any())).thenReturn(List.of(
            new DayConsumption(TODAY.minusDays(1), new Macros(150, 200, 60)),
            new DayConsumption(TODAY, new Macros(75, 100, 30))));

        var result = listDays.handle(new ListDaysQuery(null, null));

        assertThat(result.value()).hasSize(2);
        assertThat(result.value().get(0).date()).isEqualTo(TODAY);
        assertThat(result.value().get(0).caloriePercentage()).isEqualTo(50);
        assertThat(result.value().get(0).overBudget()).isFalse();
        assertThat(result.value().get(1).caloriePercentage()).isEqualTo(100);
        assertThat(result.value().get(1).overBudget()).isFalse();
    }

    @Test
    void shouldLeaveTheDaysWithoutAPercentageWhenThereIsNoPlan() {
        when(plans.findActive()).thenReturn(Optional.empty());
        when(intakes.consumptionBetween(any(), any()))
            .thenReturn(List.of(new DayConsumption(TODAY, new Macros(75, 100, 30))));

        var result = listDays.handle(new ListDaysQuery(null, null));

        assertThat(result.value().get(0).caloriePercentage()).isZero();
        assertThat(result.value().get(0).overBudget()).isFalse();
        assertThat(result.value().get(0).consumed().calories()).isEqualTo(970);
    }

    private static Plan activePlan() {
        return Plan.rehydrate(
            PlanId.of(1),
            new Weight(84_000),
            new Weight(78_000),
            new Macros(150, 200, 60),
            PlanStatus.ACTIVE,
            TODAY.minusDays(30),
            NOW);
    }

    private static Intake anIntakeOf(int protein, int carbs, int fat) {
        return Intake.rehydrate(
            INTAKE_ID, new Macros(protein, carbs, fat), Optional.empty(), TODAY, NOW);
    }
}
