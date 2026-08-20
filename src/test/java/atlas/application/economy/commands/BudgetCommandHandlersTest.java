package atlas.application.economy.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.economy.commands.changebudgetlimit.ChangeBudgetLimitCommand;
import atlas.application.economy.commands.changebudgetlimit.ChangeBudgetLimitCommandHandler;
import atlas.application.economy.commands.definebudget.DefineBudgetCommand;
import atlas.application.economy.commands.definebudget.DefineBudgetCommandHandler;
import atlas.application.economy.commands.removebudget.RemoveBudgetCommand;
import atlas.application.economy.commands.removebudget.RemoveBudgetCommandHandler;
import atlas.application.economy.ports.BudgetRepository;
import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.domain.economy.Budget;
import atlas.domain.economy.BudgetErrors;
import atlas.domain.economy.BudgetId;
import atlas.domain.economy.MovementErrors;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.events.BudgetDefinedEvent;
import atlas.domain.economy.events.BudgetLimitChangedEvent;
import atlas.domain.economy.events.BudgetRemovedEvent;
import atlas.domain.economy.vos.Money;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BudgetCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final BudgetId ID = BudgetId.of(3);

    private final EconomyUnitOfWork unitOfWork = mock(EconomyUnitOfWork.class);
    private final BudgetRepository budgets = mock(BudgetRepository.class);

    private final DefineBudgetCommandHandler define = new DefineBudgetCommandHandler(unitOfWork, CLOCK);
    private final ChangeBudgetLimitCommandHandler change = new ChangeBudgetLimitCommandHandler(unitOfWork, CLOCK);
    private final RemoveBudgetCommandHandler remove = new RemoveBudgetCommandHandler(unitOfWork, CLOCK);

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.withBudgets(unitOfWork, budgets);
        when(budgets.nextId()).thenReturn(ID);
    }

    @Test
    void shouldDefineABudgetAndReturnIt() {
        var result = define.handle(new DefineBudgetCommand(Category.FOOD, new BigDecimal("300.00")));

        assertThat(result.value().id()).isEqualTo("P00000003");
        assertThat(result.value().category()).isEqualTo("FOOD");
        assertThat(result.value().limit()).isEqualByComparingTo("300.00");

        var saved = ArgumentCaptor.forClass(Budget.class);
        verify(budgets).create(saved.capture());
        assertThat(saved.getValue().pendingEvents()).containsExactly(new BudgetDefinedEvent(ID, NOW));
    }

    @Test
    void shouldRefuseASecondBudgetForTheSameCategory() {
        when(budgets.existsFor(Category.FOOD)).thenReturn(true);

        var result = define.handle(new DefineBudgetCommand(Category.FOOD, new BigDecimal("300.00")));

        assertThat(result.error()).isEqualTo(BudgetErrors.alreadyDefinedFor(Category.FOOD));
        verify(budgets, never()).create(any());
    }

    @Test
    void shouldRefuseToBudgetIncome() {
        var result = define.handle(new DefineBudgetCommand(Category.INCOME, new BigDecimal("300.00")));

        assertThat(result.error()).isEqualTo(BudgetErrors.ONLY_SPENDING_CAN_BE_BUDGETED);
        verify(budgets, never()).create(any());
    }

    @Test
    void shouldFailWhenTheLimitIsNotPositive() {
        var result = define.handle(new DefineBudgetCommand(Category.FOOD, BigDecimal.ZERO));

        assertThat(result.error()).isEqualTo(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
        verify(budgets, never()).create(any());
    }

    @Test
    void shouldChangeTheLimit() {
        when(budgets.get(ID)).thenReturn(Optional.of(aFoodBudget()));

        var result = change.handle(new ChangeBudgetLimitCommand(ID, new BigDecimal("500.00")));

        assertThat(result.value().limit()).isEqualByComparingTo("500.00");
        verify(budgets).update(any(Budget.class));
    }

    @Test
    void shouldRegisterTheLimitChangedEvent() {
        var budget = aFoodBudget();
        when(budgets.get(ID)).thenReturn(Optional.of(budget));

        change.handle(new ChangeBudgetLimitCommand(ID, new BigDecimal("500.00")));

        assertThat(budget.pendingEvents()).contains(new BudgetLimitChangedEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenChangingABudgetThatDoesNotExist() {
        when(budgets.get(ID)).thenReturn(Optional.empty());

        var result = change.handle(new ChangeBudgetLimitCommand(ID, new BigDecimal("500.00")));

        assertThat(result.error()).isEqualTo(BudgetErrors.notFound(ID));
        verify(budgets, never()).update(any());
    }

    @Test
    void shouldRemoveTheBudget() {
        var budget = aFoodBudget();
        when(budgets.get(ID)).thenReturn(Optional.of(budget));

        var result = remove.handle(new RemoveBudgetCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(budget.pendingEvents()).containsExactly(new BudgetRemovedEvent(ID, NOW));
        verify(budgets).delete(budget);
    }

    @Test
    void shouldFailWhenRemovingABudgetThatDoesNotExist() {
        when(budgets.get(ID)).thenReturn(Optional.empty());

        assertThat(remove.handle(new RemoveBudgetCommand(ID)).error()).isEqualTo(BudgetErrors.notFound(ID));
        verify(budgets, never()).delete(any());
    }

    private static Budget aFoodBudget() {
        return Budget.rehydrate(ID, Category.FOOD, Money.ofCents(30000).value());
    }
}
