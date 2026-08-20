package atlas.presentation.economy.requests;

import atlas.presentation.economy.web.Values;
import java.math.BigDecimal;
import java.util.Map;

public record ChangeBudgetLimitRequest(BigDecimal limit) {

    public static ChangeBudgetLimitRequest from(Map<String, Object> body) {
        return new ChangeBudgetLimitRequest(Values.amount(body, "limit"));
    }
}
