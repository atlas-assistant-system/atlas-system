package atlas.application.presence.queries.listprofiles;

import atlas.application.presence.dto.ProfileSummaryDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public final class ListProfilesQueryHandler
    implements QueryHandler<ListProfilesQuery, Result<List<ProfileSummaryDto>>> {

    private final BiometricProfileRepository profiles;

    public ListProfilesQueryHandler(BiometricProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public Result<List<ProfileSummaryDto>> handle(ListProfilesQuery query) {
        return Result.success(profiles.getAll().stream().map(PresenceMapper::toSummaryDto).toList());
    }
}
