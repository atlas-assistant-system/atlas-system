package atlas.application.presence.queries.getprofile;

import atlas.application.presence.dto.ProfileDto;
import atlas.domain.presence.BiometricProfileId;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetProfileQuery(BiometricProfileId profileId) implements Query<Result<ProfileDto>> {}
