package atlas.application.presence.commands.addfacetemplate;

import atlas.application.presence.dto.ProfileDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.FaceTemplateIdGenerator;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class AddFaceTemplateCommandHandler
    implements CommandHandler<AddFaceTemplateCommand, Result<ProfileDto>> {

    private final PresenceUnitOfWork unitOfWork;
    private final FaceTemplateIdGenerator templateIds;
    private final Clock clock;

    public AddFaceTemplateCommandHandler(
        PresenceUnitOfWork unitOfWork, FaceTemplateIdGenerator templateIds, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.templateIds = templateIds;
        this.clock = clock;
    }

    @Override
    public Result<ProfileDto> handle(AddFaceTemplateCommand command) {
        var now = clock.instant();
        return unitOfWork.execute(() -> {
            var profiles = unitOfWork.profiles();
            var profile = profiles.get(command.profileId());
            if (profile.isEmpty()) {
                return Result.failure(PresenceErrors.profileNotFound(command.profileId()));
            }

            var modelResult = ModelVersion.create(command.modelVersion());
            if (modelResult.isFailure()) {
                return Result.failure(modelResult.error());
            }

            var descriptorResult = FaceDescriptor.create(modelResult.value(), command.descriptor());
            if (descriptorResult.isFailure()) {
                return Result.failure(descriptorResult.error());
            }

            var result = profile.get().addTemplate(templateIds.next(), descriptorResult.value(), now);
            if (result.isFailure()) {
                return Result.failure(result.error());
            }

            profiles.update(profile.get());

            return Result.success(PresenceMapper.toDto(profile.get()));
        });
    }
}
