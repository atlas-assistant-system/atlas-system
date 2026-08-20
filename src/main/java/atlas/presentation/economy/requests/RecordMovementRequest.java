package atlas.presentation.economy.requests;

import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.presentation.economy.web.Values;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record RecordMovementRequest(
    MovementKind kind,
    BigDecimal amount,
    Category category,
    String note,
    LocalDate occurredOn) {

    public static RecordMovementRequest from(Map<String, Object> body) {
        return new RecordMovementRequest(
            Values.enumeration(body, "kind", MovementKind.class),
            Values.amount(body, "amount"),
            Values.enumeration(body, "category", Category.class),
            Values.text(body, "note"),
            Values.date(body, "occurredOn"));
    }
}
