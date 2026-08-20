package atlas.application.economy.queries.listsavingsgoals;

import atlas.application.economy.dto.SavingsGoalStatusDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.economy.ports.SavingsGoalReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.economy.services.SavingsProjection;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class ListSavingsGoalsQueryHandler
    implements QueryHandler<ListSavingsGoalsQuery, Result<List<SavingsGoalStatusDto>>> {

    public static final int MONTHS_OF_HISTORY = 6;

    private final SavingsGoalReadModel goals;
    private final MovementReadModel movements;
    private final SavingsProjection projection;
    private final Clock clock;

    public ListSavingsGoalsQueryHandler(
        SavingsGoalReadModel goals, MovementReadModel movements, SavingsProjection projection, Clock clock) {
        this.goals = goals;
        this.movements = movements;
        this.projection = projection;
        this.clock = clock;
    }

    @Override
    public Result<List<SavingsGoalStatusDto>> handle(ListSavingsGoalsQuery query) {
        var defined = goals.findAll();
        if (defined.isEmpty()) {
            return Result.success(List.of());
        }

        var today = LocalDate.now(clock);
        var netCents = netOverTheLastWholeMonths(today);

        return Result.success(EconomyMapper.toSavingsGoals(defined, goal -> projection.of(
            goal.target(), netCents, MONTHS_OF_HISTORY, today, goal.deadline())));
    }

    private long netOverTheLastWholeMonths(LocalDate today) {
        var firstOfThisMonth = today.withDayOfMonth(1);
        var from = firstOfThisMonth.minusMonths(MONTHS_OF_HISTORY);
        var balance = movements.balanceBetween(from, firstOfThisMonth.minusDays(1));

        return balance.incomeCents() - balance.expenseCents();
    }
}
