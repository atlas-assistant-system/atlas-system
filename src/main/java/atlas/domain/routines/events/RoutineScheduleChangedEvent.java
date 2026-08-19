package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import atlas.domain.routines.vos.Schedule;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record RoutineScheduleChangedEvent(RoutineId routineId, Schedule schedule, Instant occurredOn)
    implements DomainEvent {}
