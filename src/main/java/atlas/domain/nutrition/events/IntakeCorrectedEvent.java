package atlas.domain.nutrition.events;

import atlas.domain.nutrition.IntakeId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record IntakeCorrectedEvent(IntakeId intakeId, Instant occurredOn) implements DomainEvent {}
