package atlas.application.economy.queries.listsavingsgoals;

import atlas.application.economy.dto.SavingsGoalStatusDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public record ListSavingsGoalsQuery() implements Query<Result<List<SavingsGoalStatusDto>>> {}
