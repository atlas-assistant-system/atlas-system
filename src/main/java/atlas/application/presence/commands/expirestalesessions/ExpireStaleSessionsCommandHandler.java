package atlas.application.presence.commands.expirestalesessions;

import atlas.application.presence.ports.PresenceUnitOfWork;
import java.time.Clock;
import sharedkernel.application.cqrs.CommandHandler;
import sharedkernel.domain.results.Result;

public final class ExpireStaleSessionsCommandHandler
    implements CommandHandler<ExpireStaleSessionsCommand, Result<Integer>> {

    private final PresenceUnitOfWork unitOfWork;
    private final Clock clock;

    public ExpireStaleSessionsCommandHandler(PresenceUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Integer> handle(ExpireStaleSessionsCommand command) {
        var now = clock.instant();
        return unitOfWork.execute(() -> {
            var sessions = unitOfWork.sessions();
            var expired = 0;

            for (var session : sessions.findActive()) {
                if (session.expire(now)) {
                    sessions.update(session);
                    expired++;
                }
            }

            return Result.success(expired);
        });
    }
}
