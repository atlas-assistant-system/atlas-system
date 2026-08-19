package atlas.application.presence.commands.expirestalesessions;

import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record ExpireStaleSessionsCommand() implements Command<Result<Integer>> {}
