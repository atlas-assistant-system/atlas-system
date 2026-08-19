package atlas.application.appointments.queries.getupcomingappointments;

import atlas.application.appointments.dto.AppointmentSummaryDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.CommonErrors;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public final class GetUpcomingAppointmentsQueryHandler
    implements QueryHandler<GetUpcomingAppointmentsQuery, Result<List<AppointmentSummaryDto>>> {

    private final AppointmentReadModel appointments;

    public GetUpcomingAppointmentsQueryHandler(AppointmentReadModel appointments) {
        this.appointments = appointments;
    }

    @Override
    public Result<List<AppointmentSummaryDto>> handle(GetUpcomingAppointmentsQuery query) {
        if (query.limit() <= 0) {
            return Result.failure(CommonErrors.invalid("limit"));
        }

        var upcoming = appointments.findUpcoming(query.now(), query.limit());

        return Result.success(upcoming.stream().map(AppointmentMapper::toDto).toList());
    }
}
