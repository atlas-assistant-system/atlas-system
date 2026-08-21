package atlas.application.nutrition.queries.listintakes;

import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.util.List;

public record ListIntakesQuery(
    LocalDate from,
    LocalDate to,
    Integer limit) implements Query<Result<List<IntakeDto>>> {}
