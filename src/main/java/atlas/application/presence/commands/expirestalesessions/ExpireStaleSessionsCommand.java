package atlas.application.presence.commands.expirestalesessions;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;

public record ExpireStaleSessionsCommand() implements Command<Result<Integer>> {}
