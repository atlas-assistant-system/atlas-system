package atlas.application.economy.queries.getbreakdown;

import atlas.application.economy.dto.CategorySpendDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.economy.queries.Period;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class GetBreakdownQueryHandler
    implements QueryHandler<GetBreakdownQuery, Result<List<CategorySpendDto>>> {

    private final MovementReadModel movements;
    private final Clock clock;

    public GetBreakdownQueryHandler(MovementReadModel movements, Clock clock) {
        this.movements = movements;
        this.clock = clock;
    }

    @Override
    public Result<List<CategorySpendDto>> handle(GetBreakdownQuery query) {
        var period = Period.of(query.from(), query.to(), LocalDate.now(clock));

        return Result.success(
            EconomyMapper.toBreakdown(movements.spendingBetween(period.from(), period.to())));
    }
}
