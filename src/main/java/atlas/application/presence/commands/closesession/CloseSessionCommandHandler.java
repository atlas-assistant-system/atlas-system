package atlas.application.presence.commands.closesession;

import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class CloseSessionCommandHandler
    implements CommandHandler<CloseSessionCommand, Result<Void>> {

    private final PresenceUnitOfWork unitOfWork;
    private final Clock clock;

    public CloseSessionCommandHandler(PresenceUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(CloseSessionCommand command) {
        var now = clock.instant();
        return unitOfWork.execute(() -> {
            var sessions = unitOfWork.sessions();
            var session = sessions.get(command.sessionId());
            if (session.isEmpty()) {
                return Result.failure(PresenceErrors.sessionNotFound(command.sessionId()));
            }

            session.get().close(now);
            sessions.update(session.get());

            return Result.success();
        });
    }
}
