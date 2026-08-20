package atlas.presentation.economy.requests;

import atlas.domain.economy.enums.Category;
import atlas.presentation.economy.web.Values;
import java.util.Map;

public record RecategorizeMovementRequest(Category category) {

    public static RecategorizeMovementRequest from(Map<String, Object> body) {
        return new RecategorizeMovementRequest(Values.enumeration(body, "category", Category.class));
    }
}
