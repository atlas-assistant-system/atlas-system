package atlas.presentation.routines.requests;

import atlas.presentation.routines.web.Values;
import java.util.Map;

public record ChangeRoutineDetailsRequest(String name, String description) {

    public static ChangeRoutineDetailsRequest from(Map<String, Object> body) {
        return new ChangeRoutineDetailsRequest(Values.text(body, "name"), Values.text(body, "description"));
    }
}
