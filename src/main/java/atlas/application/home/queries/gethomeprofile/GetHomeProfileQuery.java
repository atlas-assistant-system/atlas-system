package atlas.application.home.queries.gethomeprofile;

import atlas.application.home.dto.HomeProfileDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.home.HomeProfileId;
import atlas.domain.sharedkernel.results.Result;

public record GetHomeProfileQuery(HomeProfileId profileId) implements Query<Result<HomeProfileDto>> {}
