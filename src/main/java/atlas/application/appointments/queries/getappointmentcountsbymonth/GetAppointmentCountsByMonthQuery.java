package atlas.application.appointments.queries.getappointmentcountsbymonth;

import atlas.application.appointments.dto.MonthlyAppointmentCountDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.Year;
import java.util.List;

public record GetAppointmentCountsByMonthQuery(Year year, boolean includeCancelled)
    implements Query<Result<List<MonthlyAppointmentCountDto>>> {}
