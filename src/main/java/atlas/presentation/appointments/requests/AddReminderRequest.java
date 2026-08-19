package atlas.presentation.appointments.requests;

import atlas.presentation.appointments.web.Values;
import java.util.Map;

public record AddReminderRequest(int leadTimeMinutes) {

    public static AddReminderRequest from(Map<String, Object> body) {
        return new AddReminderRequest(Values.integer(body, "leadTimeMinutes"));
    }
}
