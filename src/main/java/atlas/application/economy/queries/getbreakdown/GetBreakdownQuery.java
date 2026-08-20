package atlas.application.economy.queries.getbreakdown;

import atlas.application.economy.dto.CategorySpendDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.util.List;

public record GetBreakdownQuery(LocalDate from, LocalDate to) implements Query<Result<List<CategorySpendDto>>> {}
