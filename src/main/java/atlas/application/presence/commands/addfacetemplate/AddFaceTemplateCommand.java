package atlas.application.presence.commands.addfacetemplate;

import atlas.application.presence.dto.ProfileDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.sharedkernel.results.Result;

public record AddFaceTemplateCommand(
    BiometricProfileId profileId, String modelVersion, float[] descriptor)
    implements Command<Result<ProfileDto>> {}
