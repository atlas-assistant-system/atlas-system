package atlas.presentation.nutrition.requests;

import atlas.presentation.nutrition.web.Values;
import java.math.BigDecimal;
import java.util.Map;

public record AdjustPlanRequest(BigDecimal targetWeight, int protein, int carbs, int fat) {

    public static AdjustPlanRequest from(Map<String, Object> body) {
        Values.rejectCalories(body);

        return new AdjustPlanRequest(
            Values.kilograms(body, "targetWeight"),
            Values.grams(body, "protein"),
            Values.grams(body, "carbs"),
            Values.grams(body, "fat"));
    }
}
