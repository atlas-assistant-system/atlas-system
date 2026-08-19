package atlas.application.presence.commands.enrollprofile;

import atlas.application.presence.dto.ProfileDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.FaceTemplateIdGenerator;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.ProfileName;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class EnrollProfileCommandHandler
    implements CommandHandler<EnrollProfileCommand, Result<ProfileDto>> {

    private final PresenceUnitOfWork unitOfWork;
    private final FaceTemplateIdGenerator templateIds;
    private final Clock clock;
    private final boolean maintenanceMode;

    public EnrollProfileCommandHandler(
        PresenceUnitOfWork unitOfWork,
        FaceTemplateIdGenerator templateIds,
        Clock clock,
        boolean maintenanceMode) {
        this.unitOfWork = unitOfWork;
        this.templateIds = templateIds;
        this.clock = clock;
        this.maintenanceMode = maintenanceMode;
    }

    @Override
    public Result<ProfileDto> handle(EnrollProfileCommand command) {
        var nameResult = ProfileName.create(command.displayName());
        if (nameResult.isFailure()) {
            return Result.failure(nameResult.error());
        }

        var modelResult = ModelVersion.create(command.modelVersion());
        if (modelResult.isFailure()) {
            return Result.failure(modelResult.error());
        }

        var descriptorResult = FaceDescriptor.create(modelResult.value(), command.descriptor());
        if (descriptorResult.isFailure()) {
            return Result.failure(descriptorResult.error());
        }

        var now = clock.instant();
        return unitOfWork.execute(() -> {
            if (!maintenanceMode
                && unitOfWork.sessions().findActive().stream().noneMatch(session -> session.isActive(now))) {
                return Result.failure(PresenceErrors.INTERACTION_REQUIRES_SESSION);
            }

            var profiles = unitOfWork.profiles();
            var profile = BiometricProfile.enroll(
                profiles.nextId(), nameResult.value(), templateIds.next(), descriptorResult.value(), now);
            profiles.create(profile);

            return Result.success(PresenceMapper.toDto(profile));
        });
    }
}
