package atlas.application.presence.queries.getactivesession;

import atlas.application.presence.dto.SessionDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;
import java.util.Optional;

public final class GetActiveSessionQueryHandler
    implements QueryHandler<GetActiveSessionQuery, Result<Optional<SessionDto>>> {

    private final AuthenticationSessionRepository sessions;
    private final Clock clock;

    public GetActiveSessionQueryHandler(AuthenticationSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public Result<Optional<SessionDto>> handle(GetActiveSessionQuery query) {
        var now = clock.instant();
        var active = sessions.findActive().stream()
            .filter(session -> session.isActive(now))
            .findFirst()
            .map(PresenceMapper::toDto);

        return Result.success(active);
    }
}
