package atlas.presentation.nutrition.requests;

import atlas.presentation.nutrition.web.Values;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record DefinePlanRequest(
    BigDecimal startWeight,
    BigDecimal targetWeight,
    int calories,
    int protein,
    int carbs,
    int fat,
    LocalDate startedOn) {

    public static DefinePlanRequest from(Map<String, Object> body) {
        return new DefinePlanRequest(
            Values.kilograms(body, "startWeight"),
            Values.kilograms(body, "targetWeight"),
            Values.calories(body),
            Values.grams(body, "protein"),
            Values.grams(body, "carbs"),
            Values.grams(body, "fat"),
            Values.date(body, "startedOn"));
    }
}
