package atlas.application.nutrition.queries.getactiveplan;

import atlas.application.nutrition.dto.PlanDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;

public record GetActivePlanQuery() implements Query<Result<PlanDto>> {}
