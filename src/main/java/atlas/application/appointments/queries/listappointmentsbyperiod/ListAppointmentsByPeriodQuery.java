package atlas.application.appointments.queries.listappointmentsbyperiod;

import atlas.application.appointments.dto.AppointmentPageDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.application.sharedkernel.paging.PageRequest;
import atlas.domain.appointments.enums.CalendarPeriod;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;

public record ListAppointmentsByPeriodQuery(
    CalendarPeriod period, LocalDate anchor, boolean includeCancelled, PageRequest pageRequest)
    implements Query<Result<AppointmentPageDto>> {}
