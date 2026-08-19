package atlas.application.appointments.queries.findfreeslots;

import atlas.application.appointments.dto.FreeSlotDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record FindFreeSlotsQuery(LocalDate day, LocalTime windowStart, LocalTime windowEnd, int minDurationMinutes)
    implements Query<Result<List<FreeSlotDto>>> {}
