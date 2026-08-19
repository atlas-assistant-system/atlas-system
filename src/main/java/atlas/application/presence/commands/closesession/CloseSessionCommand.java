package atlas.application.presence.commands.closesession;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.presence.SessionId;
import atlas.domain.sharedkernel.results.Result;

public record CloseSessionCommand(SessionId sessionId) implements Command<Result<Void>> {}
