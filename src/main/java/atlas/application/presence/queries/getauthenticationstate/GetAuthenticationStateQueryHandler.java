package atlas.application.presence.queries.getauthenticationstate;

import atlas.application.presence.dto.AuthenticationStateDto;
import atlas.application.presence.mappers.PresenceMapper;
import atlas.application.presence.ports.AuthenticationGateRepository;
import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.application.presence.ports.BiometricProfileRepository;
import java.time.Clock;
import sharedkernel.application.cqrs.QueryHandler;
import sharedkernel.domain.results.Result;

public final class GetAuthenticationStateQueryHandler
    implements QueryHandler<GetAuthenticationStateQuery, Result<AuthenticationStateDto>> {

    private final AuthenticationGateRepository gates;
    private final BiometricProfileRepository profiles;
    private final AuthenticationSessionRepository sessions;
    private final Clock clock;

    public GetAuthenticationStateQueryHandler(
        AuthenticationGateRepository gates,
        BiometricProfileRepository profiles,
        AuthenticationSessionRepository sessions,
        Clock clock) {
        this.gates = gates;
        this.profiles = profiles;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    public Result<AuthenticationStateDto> handle(GetAuthenticationStateQuery query) {
        var now = clock.instant();
        var gate = gates.get();
        var activeSession = sessions.findActive().stream()
            .filter(session -> session.isActive(now))
            .findFirst()
            .map(PresenceMapper::toDto)
            .orElse(null);

        return Result.success(new AuthenticationStateDto(
            gate.failedAttempts(),
            profiles.getAll().size(),
            activeSession));
    }
}
