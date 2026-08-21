package atlas.application.nutrition.commands.deleteintake;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.nutrition.IntakeId;
import atlas.domain.sharedkernel.results.Result;

public record DeleteIntakeCommand(IntakeId intakeId) implements Command<Result<Void>> {}
