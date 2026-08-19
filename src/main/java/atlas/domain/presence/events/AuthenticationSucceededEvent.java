package atlas.domain.presence.events;

import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.SessionId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record AuthenticationSucceededEvent(BiometricProfileId profileId, SessionId sessionId, Instant occurredOn)
    implements DomainEvent {}
