package atlas.application.presence.commands.addfacetemplate;

import atlas.application.presence.dto.ProfileDto;
import atlas.domain.presence.BiometricProfileId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record AddFaceTemplateCommand(
    BiometricProfileId profileId, String modelVersion, float[] descriptor)
    implements Command<Result<ProfileDto>> {}
