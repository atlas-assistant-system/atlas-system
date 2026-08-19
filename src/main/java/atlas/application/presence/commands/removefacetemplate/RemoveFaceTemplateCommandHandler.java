package atlas.application.presence.commands.removefacetemplate;

import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class RemoveFaceTemplateCommandHandler
    implements CommandHandler<RemoveFaceTemplateCommand, Result<Void>> {

    private final PresenceUnitOfWork unitOfWork;
    private final Clock clock;

    public RemoveFaceTemplateCommandHandler(PresenceUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(RemoveFaceTemplateCommand command) {
        var now = clock.instant();
        return unitOfWork.execute(() -> {
            var profiles = unitOfWork.profiles();
            var profile = profiles.get(command.profileId());
            if (profile.isEmpty()) {
                return Result.failure(PresenceErrors.profileNotFound(command.profileId()));
            }

            var result = profile.get().removeTemplate(command.templateId(), now);
            if (result.isFailure()) {
                return result;
            }

            profiles.update(profile.get());

            return Result.success();
        });
    }
}
