package atlas.presentation.economy.requests;

import atlas.presentation.economy.web.Values;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CorrectMovementRequest(BigDecimal amount, String note, LocalDate occurredOn) {

    public static CorrectMovementRequest from(Map<String, Object> body) {
        return new CorrectMovementRequest(
            Values.amount(body, "amount"),
            Values.text(body, "note"),
            Values.date(body, "occurredOn"));
    }
}
