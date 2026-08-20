package atlas.application.economy.queries.getbalance;

import atlas.application.economy.dto.BalanceDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;

public record GetBalanceQuery(LocalDate from, LocalDate to) implements Query<Result<BalanceDto>> {}
