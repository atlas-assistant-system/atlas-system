package atlas.presentation.economy.requests;

import atlas.domain.economy.enums.Category;
import atlas.presentation.economy.web.Values;
import java.math.BigDecimal;
import java.util.Map;

public record DefineBudgetRequest(Category category, BigDecimal limit) {

    public static DefineBudgetRequest from(Map<String, Object> body) {
        return new DefineBudgetRequest(
            Values.enumeration(body, "category", Category.class),
            Values.amount(body, "limit"));
    }
}
