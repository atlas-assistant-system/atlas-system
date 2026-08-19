package atlas.application.appointments.queries.getappointmentcountsbymonth;

import atlas.application.appointments.dto.MonthlyAppointmentCountDto;
import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.MonthlyAppointmentCount;
import atlas.application.sharedkernel.cqrs.QueryHandler;
import atlas.domain.appointments.enums.CalendarPeriod;
import atlas.domain.sharedkernel.results.Result;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class GetAppointmentCountsByMonthQueryHandler
    implements QueryHandler<GetAppointmentCountsByMonthQuery, Result<List<MonthlyAppointmentCountDto>>> {

    private final AppointmentReadModel appointments;

    public GetAppointmentCountsByMonthQueryHandler(AppointmentReadModel appointments) {
        this.appointments = appointments;
    }

    @Override
    public Result<List<MonthlyAppointmentCountDto>> handle(GetAppointmentCountsByMonthQuery query) {
        var window = CalendarPeriod.YEAR.windowFor(query.year().atDay(1));
        var counts = appointments.countByMonth(window, query.includeCancelled());

        var countsByMonth = counts.stream()
            .collect(Collectors.toMap(MonthlyAppointmentCount::month, MonthlyAppointmentCount::count));

        return Result.success(IntStream.rangeClosed(1, 12)
            .mapToObj(month -> YearMonth.of(query.year().getValue(), month))
            .map(month -> new MonthlyAppointmentCountDto(month, countsByMonth.getOrDefault(month, 0L)))
            .toList());
    }
}
