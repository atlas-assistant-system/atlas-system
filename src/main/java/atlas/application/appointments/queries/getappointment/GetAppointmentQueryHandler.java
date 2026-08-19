package atlas.application.appointments.queries.getappointment;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentRepository;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.results.Result;

public final class GetAppointmentQueryHandler
    implements QueryHandler<GetAppointmentQuery, Result<AppointmentDto>> {

    private final AppointmentRepository appointments;

    public GetAppointmentQueryHandler(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    @Override
    public Result<AppointmentDto> handle(GetAppointmentQuery query) {
        var appointment = appointments.get(query.appointmentId());
        if (appointment.isEmpty()) {
            return Result.failure(AppointmentErrors.notFound(query.appointmentId()));
        }

        return Result.success(AppointmentMapper.toDto(appointment.get()));
    }
}
