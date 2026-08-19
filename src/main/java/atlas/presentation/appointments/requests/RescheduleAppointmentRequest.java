package atlas.presentation.appointments.requests;

import atlas.presentation.appointments.web.Values;
import java.time.LocalDateTime;
import java.util.Map;

public record RescheduleAppointmentRequest(LocalDateTime newStart, LocalDateTime newEnd, boolean allowOverlap) {

    public static RescheduleAppointmentRequest from(Map<String, Object> body) {
        return new RescheduleAppointmentRequest(
            Values.dateTime(body, "newStart"), Values.dateTime(body, "newEnd"), Values.flag(body, "allowOverlap"));
    }
}
