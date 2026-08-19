package atlas.application.appointments.queries.findoverlappingappointments;

import atlas.application.appointments.dto.AppointmentSummaryDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;

public final class FindOverlappingAppointmentsQueryHandler
    implements QueryHandler<FindOverlappingAppointmentsQuery, Result<List<AppointmentSummaryDto>>> {

    private final AppointmentReadModel appointments;

    public FindOverlappingAppointmentsQueryHandler(AppointmentReadModel appointments) {
        this.appointments = appointments;
    }

    @Override
    public Result<List<AppointmentSummaryDto>> handle(FindOverlappingAppointmentsQuery query) {
        var slotResult = TimeSlot.create(query.start(), query.end());
        if (slotResult.isFailure()) {
            return Result.failure(slotResult.error());
        }

        var overlapping = appointments.findOverlapping(slotResult.value(), query.excludeAppointmentId());

        return Result.success(overlapping.stream().map(AppointmentMapper::toDto).toList());
    }
}
