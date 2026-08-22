package atlas.domain.training.events;

import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.training.WorkoutLogId;
import java.time.Instant;

public record SetRemovedEvent(WorkoutLogId workoutLogId, Instant occurredOn) implements DomainEvent {}
