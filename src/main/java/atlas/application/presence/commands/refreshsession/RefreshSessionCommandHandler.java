package atlas.application.presence.commands.refreshsession;

import atlas.application.presence.dto.SessionDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.vos.SessionDuration;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class RefreshSessionCommandHandler
    implements CommandHandler<RefreshSessionCommand, Result<SessionDto>> {

    private final PresenceUnitOfWork unitOfWork;
    private final SessionDuration duration;
    private final Clock clock;

    public RefreshSessionCommandHandler(
        PresenceUnitOfWork unitOfWork, SessionDuration duration, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.duration = duration;
        this.clock = clock;
    }

    @Override
    public Result<SessionDto> handle(RefreshSessionCommand command) {
        var now = clock.instant();
        return unitOfWork.execute(() -> {
            var sessions = unitOfWork.sessions();
            var session = sessions.get(command.sessionId());
            if (session.isEmpty()) {
                return Result.failure(PresenceErrors.sessionNotFound(command.sessionId()));
            }

            var result = session.get().refresh(duration, now);
            if (result.isFailure()) {
                return Result.failure(result.error());
            }

            sessions.update(session.get());

            return Result.success(PresenceMapper.toDto(session.get()));
        });
    }
}
