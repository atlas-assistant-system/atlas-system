package atlas.application.presence.queries.listprofiles;

import atlas.application.presence.dto.ProfileSummaryDto;
import java.util.List;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record ListProfilesQuery() implements Query<Result<List<ProfileSummaryDto>>> {}
