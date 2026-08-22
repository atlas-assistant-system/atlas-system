package atlas.application.economy.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.economy.ports.BudgetReadModel;
import atlas.application.economy.ports.CategorySpend;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.economy.queries.listbudgets.ListBudgetsQuery;
import atlas.application.economy.queries.listbudgets.ListBudgetsQueryHandler;
import atlas.domain.economy.Budget;
import atlas.domain.economy.BudgetId;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.services.BudgetPace;
import atlas.domain.economy.vos.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListBudgetsQueryHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-10T10:15:30Z");
    private static final LocalDate FIRST_OF_MONTH = LocalDate.of(2026, 8, 1);
    private static final LocalDate LAST_OF_MONTH = LocalDate.of(2026, 8, 31);

    private final BudgetReadModel budgets = mock(BudgetReadModel.class);
    private final MovementReadModel movements = mock(MovementReadModel.class);
    private final ListBudgetsQueryHandler handler =
        new ListBudgetsQueryHandler(budgets, movements, new BudgetPace(), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldCrossEachBudgetWithWhatWasSpentThisMonth() {
        when(budgets.findAll()).thenReturn(List.of(budgetOf(Category.FOOD, 20000)));
        when(movements.spendingBetween(FIRST_OF_MONTH, LAST_OF_MONTH))
            .thenReturn(List.of(new CategorySpend(Category.FOOD, 18000)));

        var result = handler.handle(new ListBudgetsQuery());

        assertThat(result.value()).singleElement().satisfies(budget -> {
            assertThat(budget.category()).isEqualTo("FOOD");
            assertThat(budget.label()).isEqualTo("Comida");
            assertThat(budget.limit()).isEqualByComparingTo("200.00");
            assertThat(budget.spent()).isEqualByComparingTo("180.00");
            assertThat(budget.projected()).isEqualByComparingTo("558.00");
            assertThat(budget.status()).isEqualTo("AT_RISK");
        });
    }

    @Test
    void shouldReportABudgetedCategoryWithNoSpendingAsUntouched() {
        when(budgets.findAll()).thenReturn(List.of(budgetOf(Category.LEISURE, 5000)));
        when(movements.spendingBetween(FIRST_OF_MONTH, LAST_OF_MONTH)).thenReturn(List.of());

        var result = handler.handle(new ListBudgetsQuery());

        assertThat(result.value()).singleElement().satisfies(budget -> {
            assertThat(budget.spent()).isEqualByComparingTo("0.00");
            assertThat(budget.projected()).isEqualByComparingTo("0.00");
            assertThat(budget.status()).isEqualTo("WITHIN");
        });
    }

    @Test
    void shouldIgnoreSpendingOnCategoriesWithoutABudget() {
        when(budgets.findAll()).thenReturn(List.of(budgetOf(Category.FOOD, 20000)));
        when(movements.spendingBetween(FIRST_OF_MONTH, LAST_OF_MONTH)).thenReturn(List.of(
            new CategorySpend(Category.FOOD, 1000),
            new CategorySpend(Category.HEALTH, 9999)));

        assertThat(handler.handle(new ListBudgetsQuery()).value()).hasSize(1);
    }

    @Test
    void shouldPutTheMostCommittedBudgetFirst() {
        when(budgets.findAll()).thenReturn(List.of(
            budgetOf(Category.FOOD, 20000), budgetOf(Category.LEISURE, 10000)));
        when(movements.spendingBetween(FIRST_OF_MONTH, LAST_OF_MONTH)).thenReturn(List.of(
            new CategorySpend(Category.FOOD, 2000),
            new CategorySpend(Category.LEISURE, 9000)));

        var result = handler.handle(new ListBudgetsQuery());

        assertThat(result.value()).extracting("category").containsExactly("LEISURE", "FOOD");
    }

    @Test
    void shouldReturnNothingWhenNoBudgetIsDefined() {
        when(budgets.findAll()).thenReturn(List.of());

        assertThat(handler.handle(new ListBudgetsQuery()).value()).isEmpty();
    }

    private static Budget budgetOf(Category category, long limitCents) {
        return Budget.rehydrate(BudgetId.of(1), category, Money.ofCents(limitCents).value());
    }
}
