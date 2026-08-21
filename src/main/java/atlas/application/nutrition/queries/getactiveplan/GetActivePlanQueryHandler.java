package atlas.application.nutrition.queries.getactiveplan;

import atlas.application.nutrition.dto.PlanDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.PlanReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.nutrition.PlanErrors;
import atlas.domain.sharedkernel.results.Result;

public final class GetActivePlanQueryHandler implements QueryHandler<GetActivePlanQuery, Result<PlanDto>> {

    private final PlanReadModel plans;

    public GetActivePlanQueryHandler(PlanReadModel plans) {
        this.plans = plans;
    }

    @Override
    public Result<PlanDto> handle(GetActivePlanQuery query) {
        return plans.findActive()
            .map(plan -> Result.success(NutritionMapper.toDto(plan)))
            .orElseGet(() -> Result.failure(PlanErrors.NONE_ACTIVE));
    }
}
