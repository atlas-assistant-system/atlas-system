package atlas.application.appointments.queries.findfreeslots;

import atlas.application.appointments.dto.FreeSlotDto;
import atlas.application.appointments.mappers.AppointmentMapper;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.appointments.services.AppointmentAvailability;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.results.CommonErrors;
import atlas.domain.sharedkernel.results.Result;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class FindFreeSlotsQueryHandler
    implements QueryHandler<FindFreeSlotsQuery, Result<List<FreeSlotDto>>> {

    private final AppointmentReadModel appointments;
    private final AppointmentAvailability availability;

    public FindFreeSlotsQueryHandler(AppointmentReadModel appointments, AppointmentAvailability availability) {
        this.appointments = appointments;
        this.availability = availability;
    }

    @Override
    public Result<List<FreeSlotDto>> handle(FindFreeSlotsQuery query) {
        if (query.minDurationMinutes() <= 0) {
            return Result.failure(CommonErrors.invalid("minDurationMinutes"));
        }

        var windowResult =
            TimeSlot.create(query.day().atTime(query.windowStart()), query.day().atTime(query.windowEnd()));
        if (windowResult.isFailure()) {
            return Result.failure(windowResult.error());
        }

        var booked = appointments.findBookedSlots(windowResult.value(), Optional.empty());
        var free = availability.findFreeSlots(
            windowResult.value(), Duration.ofMinutes(query.minDurationMinutes()), booked);

        return Result.success(free.stream().map(AppointmentMapper::toFreeSlotDto).toList());
    }
}
