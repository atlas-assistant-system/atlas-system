package atlas.domain.training.events;

import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.training.WorkoutId;
import java.time.Instant;

public record WorkoutArchivedEvent(WorkoutId workoutId, Instant occurredOn) implements DomainEvent {}
