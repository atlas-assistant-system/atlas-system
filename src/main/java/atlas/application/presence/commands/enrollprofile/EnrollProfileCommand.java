package atlas.application.presence.commands.enrollprofile;

import atlas.application.presence.dto.ProfileDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;

public record EnrollProfileCommand(String displayName, String modelVersion, float[] descriptor)
    implements Command<Result<ProfileDto>> {}
