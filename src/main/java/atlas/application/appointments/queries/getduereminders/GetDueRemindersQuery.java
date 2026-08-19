package atlas.application.appointments.queries.getduereminders;

import atlas.application.appointments.dto.DueReminderDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDateTime;
import java.util.List;

public record GetDueRemindersQuery(LocalDateTime now) implements Query<Result<List<DueReminderDto>>> {}
