package atlas.application.presence.queries.getprofile;

import atlas.application.presence.dto.ProfileDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.sharedkernel.results.Result;

public record GetProfileQuery(BiometricProfileId profileId) implements Query<Result<ProfileDto>> {}
