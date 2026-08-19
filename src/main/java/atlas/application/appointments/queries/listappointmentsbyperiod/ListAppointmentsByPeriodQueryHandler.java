package atlas.application.appointments.queries.listappointmentsbyperiod;

import atlas.application.appointments.dto.AppointmentPageDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.sharedkernel.results.Result;

public final class ListAppointmentsByPeriodQueryHandler
    implements QueryHandler<ListAppointmentsByPeriodQuery, Result<AppointmentPageDto>> {

    private final AppointmentReadModel appointments;

    public ListAppointmentsByPeriodQueryHandler(AppointmentReadModel appointments) {
        this.appointments = appointments;
    }

    @Override
    public Result<AppointmentPageDto> handle(ListAppointmentsByPeriodQuery query) {
        var window = query.period().windowFor(query.anchor());
        var page = appointments.findInWindow(window, query.includeCancelled(), query.pageRequest());

        return Result.success(new AppointmentPageDto(
            page.map(AppointmentMapper::toDto),
            query.anchor(),
            query.period().previous(query.anchor()),
            query.period().next(query.anchor())));
    }
}
