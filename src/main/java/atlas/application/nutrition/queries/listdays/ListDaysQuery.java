package atlas.application.nutrition.queries.listdays;

import atlas.application.nutrition.dto.DaySummaryDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.util.List;

public record ListDaysQuery(LocalDate from, LocalDate to) implements Query<Result<List<DaySummaryDto>>> {}
