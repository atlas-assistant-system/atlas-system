package atlas.domain.presence.events;

import atlas.domain.presence.enums.VerificationOutcome;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record AuthenticationFailedEvent(VerificationOutcome outcome, Instant occurredOn) implements DomainEvent {}
