package atlas.application.appointments.commands.scheduleappointment;

import atlas.application.appointments.dto.AppointmentDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDateTime;
import java.util.List;

public record ScheduleAppointmentCommand(
    String title,
    String description,
    LocalDateTime start,
    LocalDateTime end,
    List<Integer> reminderLeadTimesMinutes,
    boolean allowOverlap) implements Command<Result<AppointmentDto>> {}
