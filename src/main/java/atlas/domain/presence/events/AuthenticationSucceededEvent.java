package atlas.domain.presence.events;

import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.SessionId;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record AuthenticationSucceededEvent(BiometricProfileId profileId, SessionId sessionId, Instant occurredOn)
    implements DomainEvent {}
