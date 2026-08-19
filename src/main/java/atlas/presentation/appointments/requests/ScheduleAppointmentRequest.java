package atlas.presentation.appointments.requests;

import atlas.presentation.appointments.web.Values;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ScheduleAppointmentRequest(
    String title,
    String description,
    LocalDateTime start,
    LocalDateTime end,
    List<Integer> reminderLeadTimesMinutes,
    boolean allowOverlap) {

    public static ScheduleAppointmentRequest from(Map<String, Object> body) {
        return new ScheduleAppointmentRequest(
            Values.text(body, "title"),
            Values.text(body, "description"),
            Values.dateTime(body, "start"),
            Values.dateTime(body, "end"),
            Values.integers(body, "reminderLeadTimesMinutes"),
            Values.flag(body, "allowOverlap"));
    }
}
