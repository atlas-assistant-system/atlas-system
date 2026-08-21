package atlas.application.nutrition.queries.getintake;

import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.nutrition.mappers.NutritionMapper;
import atlas.application.nutrition.ports.IntakeReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.nutrition.IntakeErrors;
import atlas.domain.sharedkernel.results.Result;

public final class GetIntakeQueryHandler implements QueryHandler<GetIntakeQuery, Result<IntakeDto>> {

    private final IntakeReadModel intakes;

    public GetIntakeQueryHandler(IntakeReadModel intakes) {
        this.intakes = intakes;
    }

    @Override
    public Result<IntakeDto> handle(GetIntakeQuery query) {
        return intakes.find(query.intakeId())
            .map(intake -> Result.success(NutritionMapper.toDto(intake)))
            .orElseGet(() -> Result.failure(IntakeErrors.notFound(query.intakeId())));
    }
}
