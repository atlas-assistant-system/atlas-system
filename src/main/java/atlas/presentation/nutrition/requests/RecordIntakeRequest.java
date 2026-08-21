package atlas.presentation.nutrition.requests;

import atlas.presentation.nutrition.web.Values;
import java.time.LocalDate;
import java.util.Map;

public record RecordIntakeRequest(int protein, int carbs, int fat, String note, LocalDate consumedOn) {

    public static RecordIntakeRequest from(Map<String, Object> body) {
        Values.rejectCalories(body);

        return new RecordIntakeRequest(
            Values.grams(body, "protein"),
            Values.grams(body, "carbs"),
            Values.grams(body, "fat"),
            Values.text(body, "note"),
            Values.date(body, "consumedOn"));
    }
}
