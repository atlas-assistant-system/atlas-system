package atlas.application.presence.commands.closesession;

import atlas.domain.presence.SessionId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record CloseSessionCommand(SessionId sessionId) implements Command<Result<Void>> {}
