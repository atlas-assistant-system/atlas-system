package atlas.application.presence.commands.refreshsession;

import atlas.application.presence.dto.SessionDto;
import atlas.domain.presence.SessionId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record RefreshSessionCommand(SessionId sessionId) implements Command<Result<SessionDto>> {}
