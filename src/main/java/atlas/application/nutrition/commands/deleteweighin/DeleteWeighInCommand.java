package atlas.application.nutrition.commands.deleteweighin;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.nutrition.WeighInId;
import atlas.domain.sharedkernel.results.Result;

public record DeleteWeighInCommand(WeighInId weighInId) implements Command<Result<Void>> {}
