package atlas.application.economy.queries.getbalance;

import atlas.application.economy.dto.BalanceDto;
import atlas.application.economy.mappers.EconomyMapper;
import atlas.application.economy.ports.MovementReadModel;
import atlas.application.economy.queries.Period;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.time.LocalDate;

public final class GetBalanceQueryHandler implements QueryHandler<GetBalanceQuery, Result<BalanceDto>> {

    private final MovementReadModel movements;
    private final Clock clock;

    public GetBalanceQueryHandler(MovementReadModel movements, Clock clock) {
        this.movements = movements;
        this.clock = clock;
    }

    @Override
    public Result<BalanceDto> handle(GetBalanceQuery query) {
        var period = Period.of(query.from(), query.to(), LocalDate.now(clock));

        return Result.success(
            EconomyMapper.toDto(period, movements.balanceBetween(period.from(), period.to())));
    }
}
