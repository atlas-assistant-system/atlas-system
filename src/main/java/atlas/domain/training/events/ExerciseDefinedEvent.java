package atlas.domain.training.events;

import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.training.ExerciseId;
import java.time.Instant;

public record ExerciseDefinedEvent(ExerciseId exerciseId, Instant occurredOn) implements DomainEvent {}
