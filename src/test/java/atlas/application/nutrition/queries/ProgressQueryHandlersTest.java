package atlas.application.nutrition.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.nutrition.ports.PlanReadModel;
import atlas.application.nutrition.ports.WeighInReadModel;
import atlas.application.nutrition.queries.getprogress.GetProgressQuery;
import atlas.application.nutrition.queries.getprogress.GetProgressQueryHandler;
import atlas.application.nutrition.queries.listweighins.ListWeighInsQuery;
import atlas.application.nutrition.queries.listweighins.ListWeighInsQueryHandler;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.PlanErrors;
import atlas.domain.nutrition.PlanId;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.WeighInId;
import atlas.domain.nutrition.enums.PlanStatus;
import atlas.domain.nutrition.services.PlanProgress;
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

class ProgressQueryHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T07:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final PlanReadModel plans = mock(PlanReadModel.class);
    private final WeighInReadModel weighIns = mock(WeighInReadModel.class);

    private final GetProgressQueryHandler progress =
        new GetProgressQueryHandler(plans, weighIns, new PlanProgress(), CLOCK);
    private final ListWeighInsQueryHandler list = new ListWeighInsQueryHandler(weighIns, CLOCK);

    @Test
    void shouldFailWhenThereIsNoActivePlanToMeasureAgainst() {
        when(plans.findActive()).thenReturn(Optional.empty());

        assertThat(progress.handle(new GetProgressQuery()).error()).isEqualTo(PlanErrors.NONE_ACTIVE);
    }

    @Test
    void shouldReportTheProgressAgainstTheActivePlan() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(weighIns.findBetween(any(), any())).thenReturn(List.of(weighIn(1, 81_000, TODAY)));

        var result = progress.handle(new GetProgressQuery());

        assertThat(result.isSuccess()).isTrue();

        var dto = result.value();
        assertThat(dto.startWeight()).isEqualByComparingTo("84");
        assertThat(dto.currentWeight()).isEqualByComparingTo("81");
        assertThat(dto.targetWeight()).isEqualByComparingTo("78");
        assertThat(dto.goal()).isEqualTo("LOSE");
        assertThat(dto.goalLabel()).isEqualTo("Perder peso");
        assertThat(dto.remaining()).isEqualByComparingTo("-3");
        assertThat(dto.percentage()).isEqualTo(50);
        assertThat(dto.reached()).isFalse();
    }

    @Test
    void shouldAskOnlyForTheLastTwoWeeksToMeasureTheTrend() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(weighIns.findBetween(any(), any())).thenReturn(List.of(weighIn(1, 81_000, TODAY)));

        progress.handle(new GetProgressQuery());

        var from = ArgumentCaptor.forClass(LocalDate.class);
        var to = ArgumentCaptor.forClass(LocalDate.class);
        verify(weighIns).findBetween(from.capture(), to.capture());

        assertThat(from.getValue()).isEqualTo(TODAY.minusDays(13));
        assertThat(to.getValue()).isEqualTo(TODAY);
    }

    @Test
    void shouldFallBackToTheLastKnownReadingWhenTheTwoWeeksAreEmpty() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(weighIns.findBetween(any(), any())).thenReturn(List.of());
        when(weighIns.findLatest()).thenReturn(Optional.of(weighIn(1, 80_000, TODAY.minusMonths(3))));

        var dto = progress.handle(new GetProgressQuery()).value();

        assertThat(dto.currentWeight()).isEqualByComparingTo("80");
        assertThat(dto.trendPerWeek()).isEqualByComparingTo("0");
    }

    @Test
    void shouldFallBackToTheStartingWeightWhenNobodyHasEverWeighedIn() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(weighIns.findBetween(any(), any())).thenReturn(List.of());
        when(weighIns.findLatest()).thenReturn(Optional.empty());

        var dto = progress.handle(new GetProgressQuery()).value();

        assertThat(dto.currentWeight()).isEqualByComparingTo("84");
        assertThat(dto.percentage()).isZero();
    }

    @Test
    void shouldReportTheTrendInKilogramsPerWeek() {
        when(plans.findActive()).thenReturn(Optional.of(activePlan()));
        when(weighIns.findBetween(any(), any())).thenReturn(List.of(
            weighIn(1, 84_000, TODAY.minusDays(13)),
            weighIn(2, 83_000, TODAY.minusDays(5))));

        var dto = progress.handle(new GetProgressQuery()).value();

        assertThat(dto.trendPerWeek()).isEqualByComparingTo("-1");
    }

    @Test
    void shouldListTheSeriesOldestFirst() {
        when(weighIns.findBetween(any(), any())).thenReturn(List.of(
            weighIn(2, 82_000, TODAY),
            weighIn(1, 84_000, TODAY.minusDays(20))));

        var series = list.handle(new ListWeighInsQuery(null, null)).value();

        assertThat(series).extracting(dto -> dto.measuredOn().toString())
            .containsExactly(TODAY.minusDays(20).toString(), TODAY.toString());
    }

    @Test
    void shouldAskForThreeMonthsOfSeriesByDefault() {
        when(weighIns.findBetween(any(), any())).thenReturn(List.of());

        list.handle(new ListWeighInsQuery(null, null));

        var from = ArgumentCaptor.forClass(LocalDate.class);
        var to = ArgumentCaptor.forClass(LocalDate.class);
        verify(weighIns).findBetween(from.capture(), to.capture());

        assertThat(from.getValue()).isEqualTo(TODAY.minusDays(89));
        assertThat(to.getValue()).isEqualTo(TODAY);
    }

    @Test
    void shouldHonourAnExplicitRange() {
        when(weighIns.findBetween(any(), any())).thenReturn(List.of());

        list.handle(new ListWeighInsQuery(TODAY.minusDays(3), TODAY));

        verify(weighIns).findBetween(TODAY.minusDays(3), TODAY);
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

    private static WeighIn weighIn(int id, int grams, LocalDate day) {
        return WeighIn.rehydrate(WeighInId.of(id), new Weight(grams), day, NOW);
    }
}
