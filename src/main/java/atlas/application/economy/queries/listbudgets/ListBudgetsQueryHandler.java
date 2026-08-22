package atlas.application.economy.queries.listbudgets;

import atlas.application.economy.dto.BudgetStatusDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.BudgetReadModel;
import atlas.application.economy.ports.CategorySpend;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.economy.queries.Period;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.services.BudgetPace;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ListBudgetsQueryHandler implements QueryHandler<ListBudgetsQuery, Result<List<BudgetStatusDto>>> {

    private final BudgetReadModel budgets;
    private final MovementReadModel movements;
    private final BudgetPace pace;
    private final Clock clock;

    public ListBudgetsQueryHandler(
        BudgetReadModel budgets, MovementReadModel movements, BudgetPace pace, Clock clock) {
        this.budgets = budgets;
        this.movements = movements;
        this.pace = pace;
        this.clock = clock;
    }

    @Override
    public Result<List<BudgetStatusDto>> handle(ListBudgetsQuery query) {
        var defined = budgets.findAll();
        if (defined.isEmpty()) {
            return Result.success(List.of());
        }

        var today = LocalDate.now(clock);
        var month = Period.of(null, null, today);
        var spending = spendingByCategory(month.from(), month.to());

        return Result.success(EconomyMapper.toBudgets(defined, budget -> pace.of(
            budget.limit(), spending.getOrDefault(budget.category(), 0L), today)));
    }

    private Map<Category, Long> spendingByCategory(LocalDate from, LocalDate to) {
        return movements.spendingBetween(from, to).stream()
            .collect(Collectors.toMap(CategorySpend::category, CategorySpend::cents));
    }
}
