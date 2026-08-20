package atlas.domain.economy;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.economy.enums.Category;
import atlas.domain.economy.events.BudgetDefinedEvent;
import atlas.domain.economy.events.BudgetLimitChangedEvent;
import atlas.domain.economy.events.BudgetRemovedEvent;
import atlas.domain.economy.vos.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BudgetTest {

    private static final BudgetId ID = BudgetId.of(1);
    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final Money THREE_HUNDRED = Money.ofCents(30000).value();

    @Test
    void shouldDefineALimitForASpendingCategory() {
        var budget = Budget.define(ID, Category.FOOD, THREE_HUNDRED, NOW).value();

        assertThat(budget.id()).isEqualTo(ID);
        assertThat(budget.category()).isEqualTo(Category.FOOD);
        assertThat(budget.limit()).isEqualTo(THREE_HUNDRED);
        assertThat(budget.pendingEvents()).containsExactly(new BudgetDefinedEvent(ID, NOW));
    }

    @Test
    void shouldRefuseToBudgetIncome() {
        var result = Budget.define(ID, Category.INCOME, THREE_HUNDRED, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(BudgetErrors.ONLY_SPENDING_CAN_BE_BUDGETED);
    }

    @Test
    void shouldChangeTheLimit() {
        var budget = Budget.define(ID, Category.FOOD, THREE_HUNDRED, NOW).value();
        budget.clearEvents();
        var raised = Money.ofCents(50000).value();

        var result = budget.changeLimit(raised, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(budget.limit()).isEqualTo(raised);
        assertThat(budget.pendingEvents()).containsExactly(new BudgetLimitChangedEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenTheNewLimitIsTheOneItAlreadyHas() {
        var budget = Budget.define(ID, Category.FOOD, THREE_HUNDRED, NOW).value();
        budget.clearEvents();

        var result = budget.changeLimit(THREE_HUNDRED, NOW);

        assertThat(result.error()).isEqualTo(BudgetErrors.LIMIT_UNCHANGED);
        assertThat(budget.pendingEvents()).isEmpty();
    }

    @Test
    void shouldRegisterRemovedEventWhenRemoved() {
        var budget = Budget.define(ID, Category.FOOD, THREE_HUNDRED, NOW).value();
        budget.clearEvents();

        var result = budget.remove(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(budget.pendingEvents()).containsExactly(new BudgetRemovedEvent(ID, NOW));
    }

    @Test
    void shouldNotRegisterAnyEventWhenRehydratedFromStorage() {
        var budget = Budget.rehydrate(ID, Category.FOOD, THREE_HUNDRED);

        assertThat(budget.pendingEvents()).isEmpty();
        assertThat(budget.limit()).isEqualTo(THREE_HUNDRED);
    }
}
