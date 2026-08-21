package atlas.application.nutrition.commands.correctintake;

import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.nutrition.IntakeId;
import atlas.domain.sharedkernel.results.Result;

public record CorrectIntakeCommand(
    IntakeId intakeId,
    int protein,
    int carbs,
    int fat,
    String note) implements Command<Result<IntakeDto>> {}
