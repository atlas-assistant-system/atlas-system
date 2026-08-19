package atlas.domain.routines.events;

import atlas.domain.routines.RoutineId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProgressLoggedEvent(RoutineId routineId, LocalDate day, BigDecimal amount, Instant occurredOn)
    implements DomainEvent {}
