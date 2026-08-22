package atlas.domain.training.events;

import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.training.WorkoutLogId;
import java.time.Instant;

public record WorkoutStartedEvent(WorkoutLogId workoutLogId, Instant occurredOn) implements DomainEvent {}
