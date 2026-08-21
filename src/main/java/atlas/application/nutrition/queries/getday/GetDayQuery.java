package atlas.application.nutrition.queries.getday;

import atlas.application.nutrition.dto.DayDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;

public record GetDayQuery(LocalDate date) implements Query<Result<DayDto>> {}
