package atlas.presentation.nutrition.requests;

import atlas.presentation.nutrition.web.Values;
import java.util.Map;

public record CorrectIntakeRequest(int protein, int carbs, int fat, String note) {

    public static CorrectIntakeRequest from(Map<String, Object> body) {
        Values.rejectCalories(body);

        return new CorrectIntakeRequest(
            Values.grams(body, "protein"),
            Values.grams(body, "carbs"),
            Values.grams(body, "fat"),
            Values.text(body, "note"));
    }
}
