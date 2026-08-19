package atlas.application.appointments.queries.getappointmentcountsbyday;

import atlas.application.appointments.dto.DailyAppointmentCountDto;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.DailyAppointmentCount;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.appointments.enums.CalendarPeriod;
import atlas.domain.sharedkernel.results.Result;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class GetAppointmentCountsByDayQueryHandler
    implements QueryHandler<GetAppointmentCountsByDayQuery, Result<List<DailyAppointmentCountDto>>> {

    private final AppointmentReadModel appointments;

    public GetAppointmentCountsByDayQueryHandler(AppointmentReadModel appointments) {
        this.appointments = appointments;
    }

    @Override
    public Result<List<DailyAppointmentCountDto>> handle(GetAppointmentCountsByDayQuery query) {
        var window = CalendarPeriod.MONTH.windowFor(query.month().atDay(1));
        var counts = appointments.countByDay(window, query.includeCancelled());

        var countsByDay = counts.stream()
            .collect(Collectors.toMap(DailyAppointmentCount::day, DailyAppointmentCount::count));

        return Result.success(IntStream.rangeClosed(1, query.month().lengthOfMonth())
            .mapToObj(query.month()::atDay)
            .map(day -> new DailyAppointmentCountDto(day, countsByDay.getOrDefault(day, 0L)))
            .toList());
    }
}
