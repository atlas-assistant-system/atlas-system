package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record RoutineDetailsChangedEvent(RoutineId routineId, Instant occurredOn) implements DomainEvent {}
