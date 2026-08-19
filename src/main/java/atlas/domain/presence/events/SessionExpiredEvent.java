package atlas.domain.presence.events;

import atlas.domain.presence.SessionId;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record SessionExpiredEvent(SessionId sessionId, Instant occurredOn) implements DomainEvent {}
