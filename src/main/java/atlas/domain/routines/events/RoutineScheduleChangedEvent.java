package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import atlas.domain.routines.vos.Schedule;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record RoutineScheduleChangedEvent(RoutineId routineId, Schedule schedule, Instant occurredOn)
    implements DomainEvent {}
