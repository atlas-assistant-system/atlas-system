package atlas.application.appointments.queries.getduereminders;

import atlas.application.appointments.dto.DueReminderDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public final class GetDueRemindersQueryHandler
    implements QueryHandler<GetDueRemindersQuery, Result<List<DueReminderDto>>> {

    private final AppointmentReadModel appointments;

    public GetDueRemindersQueryHandler(AppointmentReadModel appointments) {
        this.appointments = appointments;
    }

    @Override
    public Result<List<DueReminderDto>> handle(GetDueRemindersQuery query) {
        var dueReminders = appointments.findDueReminders(query.now());

        return Result.success(dueReminders.stream().map(AppointmentMapper::toDto).toList());
    }
}
