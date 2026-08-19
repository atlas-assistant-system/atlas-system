package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import atlas.domain.routines.vos.RoutineName;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record RoutineDefinedEvent(RoutineId routineId, RoutineName name, Instant occurredOn) implements DomainEvent {}
