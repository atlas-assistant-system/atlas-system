package atlas.application.presence.queries.getprofile;

import atlas.application.presence.dto.ProfileDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.domain.presence.PresenceErrors;
import sharedkernel.application.cqrs.QueryHandler;
import sharedkernel.domain.results.Result;

public final class GetProfileQueryHandler implements QueryHandler<GetProfileQuery, Result<ProfileDto>> {

    private final BiometricProfileRepository profiles;

    public GetProfileQueryHandler(BiometricProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public Result<ProfileDto> handle(GetProfileQuery query) {
        var profile = profiles.get(query.profileId());
        if (profile.isEmpty()) {
            return Result.failure(PresenceErrors.profileNotFound(query.profileId()));
        }

        return Result.success(PresenceMapper.toDto(profile.get()));
    }
}
