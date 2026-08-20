package atlas.presentation.economy.responses;

import atlas.application.economy.dto.BalanceDto;
import atlas.application.economy.dto.CategorySpendDto;
import atlas.application.economy.dto.MovementDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EconomyResponses {

    private EconomyResponses() {}

    public static Map<String, Object> movement(MovementDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("kind", dto.kind());
        body.put("amount", dto.amount().toPlainString());
        body.put("category", dto.category());
        body.put("categoryLabel", dto.categoryLabel());
        body.put("categoryIcon", dto.categoryIcon());
        body.put("note", dto.note());
        body.put("occurredOn", dto.occurredOn().toString());
        body.put("recordedAt", dto.recordedAt().toString());

        return body;
    }

    public static List<Map<String, Object>> movements(List<MovementDto> movements) {
        return movements.stream().map(EconomyResponses::movement).toList();
    }

    public static Map<String, Object> balance(BalanceDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("from", dto.from().toString());
        body.put("to", dto.to().toString());
        body.put("income", dto.income().toPlainString());
        body.put("expense", dto.expense().toPlainString());
        body.put("net", dto.net().toPlainString());

        return body;
    }

    public static List<Map<String, Object>> breakdown(List<CategorySpendDto> spending) {
        return spending.stream().map(EconomyResponses::categorySpend).toList();
    }

    private static Map<String, Object> categorySpend(CategorySpendDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("category", dto.category());
        body.put("label", dto.label());
        body.put("icon", dto.icon());
        body.put("total", dto.total().toPlainString());
        body.put("percentage", dto.percentage().toPlainString());

        return body;
    }
}
