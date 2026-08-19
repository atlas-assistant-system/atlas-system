package atlas.application.presence.queries.listprofiles;

import atlas.application.presence.dto.ProfileSummaryDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public record ListProfilesQuery() implements Query<Result<List<ProfileSummaryDto>>> {}
