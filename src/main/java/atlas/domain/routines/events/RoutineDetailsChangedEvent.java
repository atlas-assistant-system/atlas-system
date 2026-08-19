package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record RoutineDetailsChangedEvent(RoutineId routineId, Instant occurredOn) implements DomainEvent {}
