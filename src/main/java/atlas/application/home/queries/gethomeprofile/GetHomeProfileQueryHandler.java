package atlas.application.home.queries.gethomeprofile;

import atlas.application.home.dto.HomeProfileDto;
import atlas.application.home.mappers.HomeMapper;
import atlas.application.home.ports.HomeProfileRepository;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.home.HomeErrors;
import atlas.domain.sharedkernel.results.Result;

public final class GetHomeProfileQueryHandler
    implements QueryHandler<GetHomeProfileQuery, Result<HomeProfileDto>> {

    private final HomeProfileRepository profiles;

    public GetHomeProfileQueryHandler(HomeProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public Result<HomeProfileDto> handle(GetHomeProfileQuery query) {
        return profiles.get(query.profileId())
            .map(profile -> Result.success(HomeMapper.toDto(profile)))
            .orElseGet(() -> Result.failure(HomeErrors.notFound(query.profileId())));
    }
}
