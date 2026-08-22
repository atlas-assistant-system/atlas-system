package atlas.presentation.nutrition.requests;

import atlas.presentation.nutrition.web.Values;
import java.math.BigDecimal;
import java.util.Map;

public record AdjustPlanRequest(
    BigDecimal targetWeight, int calories, int protein, int carbs, int fat) {

    public static AdjustPlanRequest from(Map<String, Object> body) {
        return new AdjustPlanRequest(
            Values.kilograms(body, "targetWeight"),
            Values.calories(body),
            Values.grams(body, "protein"),
            Values.grams(body, "carbs"),
            Values.grams(body, "fat"));
    }
}
