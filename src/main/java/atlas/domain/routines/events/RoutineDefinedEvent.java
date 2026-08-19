package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import atlas.domain.routines.vos.RoutineName;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record RoutineDefinedEvent(RoutineId routineId, RoutineName name, Instant occurredOn) implements DomainEvent {}
