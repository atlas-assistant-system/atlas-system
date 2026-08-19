package atlas.domain.presence.events;

import atlas.domain.presence.SessionId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record SessionClosedEvent(SessionId sessionId, Instant occurredOn) implements DomainEvent {}
