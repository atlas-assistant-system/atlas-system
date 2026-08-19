package atlas.presentation.appointments.requests;

import atlas.presentation.appointments.web.Values;
import java.util.Map;

public record RestoreAppointmentRequest(boolean allowOverlap) {

    public static RestoreAppointmentRequest from(Map<String, Object> body) {
        return new RestoreAppointmentRequest(Values.flag(body, "allowOverlap"));
    }
}
