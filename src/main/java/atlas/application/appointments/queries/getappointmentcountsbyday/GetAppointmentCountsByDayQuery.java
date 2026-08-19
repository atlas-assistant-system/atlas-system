package atlas.application.appointments.queries.getappointmentcountsbyday;

import atlas.application.appointments.dto.DailyAppointmentCountDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.YearMonth;
import java.util.List;

public record GetAppointmentCountsByDayQuery(YearMonth month, boolean includeCancelled)
    implements Query<Result<List<DailyAppointmentCountDto>>> {}
