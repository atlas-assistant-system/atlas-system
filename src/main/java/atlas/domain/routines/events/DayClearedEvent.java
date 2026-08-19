package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import java.time.Instant;
import java.time.LocalDate;
import sharedkernel.domain.events.DomainEvent;

public record DayClearedEvent(RoutineId routineId, LocalDate day, Instant occurredOn) implements DomainEvent {}
