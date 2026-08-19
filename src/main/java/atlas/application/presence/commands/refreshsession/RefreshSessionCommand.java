package atlas.application.presence.commands.refreshsession;

import atlas.application.presence.dto.SessionDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.presence.SessionId;
import atlas.domain.sharedkernel.results.Result;

public record RefreshSessionCommand(SessionId sessionId) implements Command<Result<SessionDto>> {}
