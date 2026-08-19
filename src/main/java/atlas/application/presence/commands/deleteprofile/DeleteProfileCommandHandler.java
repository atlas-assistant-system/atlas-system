package atlas.application.presence.commands.deleteprofile;

import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.domain.presence.PresenceErrors;
import java.time.Clock;
import sharedkernel.application.cqrs.CommandHandler;
import sharedkernel.domain.results.Result;

public final class DeleteProfileCommandHandler
    implements CommandHandler<DeleteProfileCommand, Result<Void>> {

    private final PresenceUnitOfWork unitOfWork;
    private final Clock clock;

    public DeleteProfileCommandHandler(PresenceUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(DeleteProfileCommand command) {
        var now = clock.instant();
        return unitOfWork.execute(() -> {
            var profiles = unitOfWork.profiles();
            var profile = profiles.get(command.profileId());
            if (profile.isEmpty()) {
                return Result.failure(PresenceErrors.profileNotFound(command.profileId()));
            }

            var sessions = unitOfWork.sessions();
            sessions.findActiveByProfile(command.profileId()).ifPresent(session -> {
                session.close(now);
                sessions.update(session);
            });
            profiles.delete(profile.get());

            return Result.success();
        });
    }
}
