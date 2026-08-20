package atlas.domain.economy.events;

import atlas.domain.economy.MovementId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record MovementDeletedEvent(MovementId movementId, Instant occurredOn) implements DomainEvent {}
