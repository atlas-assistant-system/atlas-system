package atlas.application.economy.queries.listbudgets;

import atlas.application.economy.dto.BudgetStatusDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public record ListBudgetsQuery() implements Query<Result<List<BudgetStatusDto>>> {}
