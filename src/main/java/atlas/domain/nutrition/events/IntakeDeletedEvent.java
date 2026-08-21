package atlas.domain.nutrition.events;

import atlas.domain.nutrition.IntakeId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record IntakeDeletedEvent(IntakeId intakeId, Instant occurredOn) implements DomainEvent {}
