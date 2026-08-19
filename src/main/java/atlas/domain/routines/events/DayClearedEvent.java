package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;

public record DayClearedEvent(RoutineId routineId, LocalDate day, Instant occurredOn) implements DomainEvent {}
