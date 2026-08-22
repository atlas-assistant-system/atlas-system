package atlas.application.economy.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.economy.ports.Balance;
import atlas.application.economy.ports.CategorySpend;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.economy.queries.getbalance.GetBalanceQuery;
import atlas.application.economy.queries.getbalance.GetBalanceQueryHandler;
import atlas.application.economy.queries.getbreakdown.GetBreakdownQuery;
import atlas.application.economy.queries.getbreakdown.GetBreakdownQueryHandler;
import atlas.application.economy.queries.getmovement.GetMovementQuery;
import atlas.application.economy.queries.getmovement.GetMovementQueryHandler;
import atlas.application.economy.queries.listmovements.ListMovementsQuery;
import atlas.application.economy.queries.listmovements.ListMovementsQueryHandler;
import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementErrors;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.vos.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EconomyQueryHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final LocalDate FIRST_OF_MONTH = LocalDate.of(2026, 8, 1);
    private static final LocalDate LAST_OF_MONTH = LocalDate.of(2026, 8, 31);
    private static final MovementId ID = MovementId.of(7);

    private final MovementReadModel movements = mock(MovementReadModel.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldReturnTheMovementItWasAskedFor() {
        when(movements.find(ID)).thenReturn(Optional.of(anExpenseOf(1250, Category.FOOD)));

        var result = new GetMovementQueryHandler(movements).handle(new GetMovementQuery(ID));

        assertThat(result.value().id()).isEqualTo("M00000007");
        assertThat(result.value().amount()).isEqualByComparingTo("12.50");
    }

    @Test
    void shouldFailWhenTheMovementDoesNotExist() {
        when(movements.find(ID)).thenReturn(Optional.empty());

        var result = new GetMovementQueryHandler(movements).handle(new GetMovementQuery(ID));

        assertThat(result.error()).isEqualTo(MovementErrors.notFound(ID));
    }

    @Test
    void shouldListTheLatestMovementsWithoutFilteringByDate() {
        when(movements.findLatest(null, null, null, 20))
            .thenReturn(List.of(anExpenseOf(1250, Category.FOOD)));

        var result = new ListMovementsQueryHandler(movements)
            .handle(new ListMovementsQuery(null, null, null, null));

        assertThat(result.value()).hasSize(1);
        verify(movements).findLatest(null, null, null, 20);
    }

    @Test
    void shouldCapTheNumberOfMovementsItReturns() {
        new ListMovementsQueryHandler(movements).handle(new ListMovementsQuery(null, null, null, 5000));

        verify(movements).findLatest(null, null, null, ListMovementsQueryHandler.MAX_LIMIT);
    }

    @Test
    void shouldFallBackToTheDefaultLimitWhenAskedForNothing() {
        new ListMovementsQueryHandler(movements).handle(new ListMovementsQuery(null, null, null, 0));

        verify(movements).findLatest(null, null, null, ListMovementsQueryHandler.DEFAULT_LIMIT);
    }

    @Test
    void shouldReadTheBalanceOfTheCurrentMonthWhenNoPeriodIsGiven() {
        when(movements.balanceBetween(FIRST_OF_MONTH, LAST_OF_MONTH))
            .thenReturn(new Balance(200000, 74550));

        var result = new GetBalanceQueryHandler(movements, clock).handle(new GetBalanceQuery(null, null));

        assertThat(result.value().from()).isEqualTo(FIRST_OF_MONTH);
        assertThat(result.value().to()).isEqualTo(LAST_OF_MONTH);
        assertThat(result.value().income()).isEqualByComparingTo("2000.00");
        assertThat(result.value().expense()).isEqualByComparingTo("745.50");
        assertThat(result.value().net()).isEqualByComparingTo("1254.50");
    }

    @Test
    void shouldReportANegativeNetWhenSpendingBeatsIncome() {
        when(movements.balanceBetween(FIRST_OF_MONTH, LAST_OF_MONTH))
            .thenReturn(new Balance(1000, 2500));

        var result = new GetBalanceQueryHandler(movements, clock).handle(new GetBalanceQuery(null, null));

        assertThat(result.value().net()).isEqualByComparingTo("-15.00");
    }

    @Test
    void shouldHonourThePeriodItIsGiven() {
        var from = LocalDate.of(2026, 1, 1);
        var to = LocalDate.of(2026, 12, 31);
        when(movements.balanceBetween(from, to)).thenReturn(new Balance(0, 0));

        var result = new GetBalanceQueryHandler(movements, clock).handle(new GetBalanceQuery(from, to));

        assertThat(result.value().from()).isEqualTo(from);
        assertThat(result.value().net()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldBreakSpendingDownByCategoryFromLargestToSmallest() {
        when(movements.spendingBetween(FIRST_OF_MONTH, LAST_OF_MONTH)).thenReturn(List.of(
            new CategorySpend(Category.LEISURE, 2500),
            new CategorySpend(Category.FOOD, 7500)));

        var result = new GetBreakdownQueryHandler(movements, clock).handle(new GetBreakdownQuery(null, null));

        assertThat(result.value()).extracting("category").containsExactly("FOOD", "LEISURE");
        assertThat(result.value().getFirst().label()).isEqualTo("Comida");
        assertThat(result.value().getFirst().total()).isEqualByComparingTo("75.00");
        assertThat(result.value().getFirst().percentage()).isEqualByComparingTo("75.0");
        assertThat(result.value().getLast().percentage()).isEqualByComparingTo("25.0");
    }

    @Test
    void shouldReportNoPercentagesWhenNothingWasSpent() {
        when(movements.spendingBetween(FIRST_OF_MONTH, LAST_OF_MONTH)).thenReturn(List.of());

        var result = new GetBreakdownQueryHandler(movements, clock).handle(new GetBreakdownQuery(null, null));

        assertThat(result.value()).isEmpty();
    }

    private static Movement anExpenseOf(long cents, Category category) {
        return Movement.rehydrate(
            ID, MovementKind.EXPENSE, Money.ofCents(cents).value(), category, Optional.empty(), TODAY, NOW);
    }
}
