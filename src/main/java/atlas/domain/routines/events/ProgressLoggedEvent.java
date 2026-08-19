package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import sharedkernel.domain.events.DomainEvent;

public record ProgressLoggedEvent(RoutineId routineId, LocalDate day, BigDecimal amount, Instant occurredOn)
    implements DomainEvent {}
