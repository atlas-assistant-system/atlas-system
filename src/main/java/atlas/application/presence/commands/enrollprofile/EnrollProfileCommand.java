package atlas.application.presence.commands.enrollprofile;

import atlas.application.presence.dto.ProfileDto;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record EnrollProfileCommand(String displayName, String modelVersion, float[] descriptor)
    implements Command<Result<ProfileDto>> {}
