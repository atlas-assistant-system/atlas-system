package atlas.application.nutrition.queries.getintake;

import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.nutrition.IntakeId;
import atlas.domain.sharedkernel.results.Result;

public record GetIntakeQuery(IntakeId intakeId) implements Query<Result<IntakeDto>> {}
