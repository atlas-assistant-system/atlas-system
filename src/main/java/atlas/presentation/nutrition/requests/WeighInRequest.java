package atlas.presentation.nutrition.requests;

import atlas.presentation.nutrition.web.Values;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record WeighInRequest(BigDecimal weight, LocalDate measuredOn) {

    public static WeighInRequest from(Map<String, Object> body) {
        return new WeighInRequest(Values.kilograms(body, "weight"), Values.date(body, "measuredOn"));
    }
}
