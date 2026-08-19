package atlas.presentation.routines.requests;

import atlas.presentation.routines.web.Values;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record LogProgressRequest(LocalDate day, BigDecimal amount) {

    public static LogProgressRequest from(Map<String, Object> body, LocalDate today) {
        var day = body.get("day") == null ? today : Values.date(body, "day");
        var amount = body.get("amount") == null ? BigDecimal.ONE : Values.decimal(body, "amount");

        return new LogProgressRequest(day, amount);
    }
}
