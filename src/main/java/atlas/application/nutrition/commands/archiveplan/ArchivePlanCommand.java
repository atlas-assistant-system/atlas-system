package atlas.application.nutrition.commands.archiveplan;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;

public record ArchivePlanCommand() implements Command<Result<Void>> {}
