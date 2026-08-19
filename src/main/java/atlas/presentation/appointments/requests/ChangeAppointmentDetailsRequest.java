package atlas.presentation.appointments.requests;

import atlas.presentation.appointments.web.Values;
import java.util.Map;

public record ChangeAppointmentDetailsRequest(String title, String description) {

    public static ChangeAppointmentDetailsRequest from(Map<String, Object> body) {
        return new ChangeAppointmentDetailsRequest(Values.text(body, "title"), Values.text(body, "description"));
    }
}
