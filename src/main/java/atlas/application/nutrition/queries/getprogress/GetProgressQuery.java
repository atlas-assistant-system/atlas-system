package atlas.application.nutrition.queries.getprogress;

import atlas.application.nutrition.dto.ProgressDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;

public record GetProgressQuery() implements Query<Result<ProgressDto>> {}
