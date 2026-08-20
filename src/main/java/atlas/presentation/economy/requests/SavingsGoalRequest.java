package atlas.presentation.economy.requests;

import atlas.presentation.economy.web.Values;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SavingsGoalRequest(String name, BigDecimal target, LocalDate deadline) {

    public static SavingsGoalRequest from(Map<String, Object> body) {
        return new SavingsGoalRequest(
            Values.text(body, "name"),
            Values.amount(body, "target"),
            Values.parseDate(Values.text(body, "deadline"), "deadline"));
    }
}
