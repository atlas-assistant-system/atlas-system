package atlas.presentation.nutrition.responses;

import atlas.application.nutrition.dto.DayDto;
import atlas.application.nutrition.dto.DaySummaryDto;
import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.nutrition.dto.MacrosDto;
import atlas.application.nutrition.dto.PlanDto;
import atlas.application.nutrition.dto.ProgressDto;
import atlas.application.nutrition.dto.WeighInDto;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NutritionResponses {

    private NutritionResponses() {}

    public static Map<String, Object> plan(PlanDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("startWeight", kilograms(dto.startWeight()));
        body.put("targetWeight", kilograms(dto.targetWeight()));
        body.put("goal", dto.goal());
        body.put("goalLabel", dto.goalLabel());
        body.put("dailyCalories", dto.dailyCalories());
        body.put("dailyMacros", macros(dto.dailyMacros()));
        body.put("status", dto.status());
        body.put("startedOn", dto.startedOn().toString());
        body.put("definedAt", dto.definedAt().toString());

        return body;
    }

    public static Map<String, Object> intake(IntakeDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("calories", dto.calories());
        body.put("macros", macros(dto.macros()));
        body.put("note", dto.note());
        body.put("consumedOn", dto.consumedOn().toString());
        body.put("recordedAt", dto.recordedAt().toString());

        return body;
    }

    public static List<Map<String, Object>> intakes(List<IntakeDto> intakes) {
        return intakes.stream().map(NutritionResponses::intake).toList();
    }

    public static Map<String, Object> day(DayDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("date", dto.date().toString());
        body.put("consumedCalories", dto.consumedCalories());
        body.put("consumedMacros", macros(dto.consumedMacros()));
        body.put("targetCalories", dto.targetCalories());
        body.put("targetMacros", macros(dto.targetMacros()));
        body.put("remainingCalories", dto.remainingCalories());
        body.put("remainingMacros", macros(dto.remainingMacros()));
        body.put("caloriePercentage", dto.caloriePercentage());
        body.put("lowerCalories", dto.lowerCalories());
        body.put("upperCalories", dto.upperCalories());
        body.put("withinRange", dto.withinRange());
        body.put("overBudget", dto.overBudget());
        body.put("intakes", intakes(dto.intakes()));

        return body;
    }

    public static List<Map<String, Object>> days(List<DaySummaryDto> days) {
        return days.stream().map(NutritionResponses::daySummary).toList();
    }

    public static Map<String, Object> weighIn(WeighInDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("weight", kilograms(dto.weight()));
        body.put("measuredOn", dto.measuredOn().toString());
        body.put("recordedAt", dto.recordedAt().toString());

        return body;
    }

    public static List<Map<String, Object>> weighIns(List<WeighInDto> weighIns) {
        return weighIns.stream().map(NutritionResponses::weighIn).toList();
    }

    public static Map<String, Object> progress(ProgressDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("startWeight", kilograms(dto.startWeight()));
        body.put("currentWeight", kilograms(dto.currentWeight()));
        body.put("targetWeight", kilograms(dto.targetWeight()));
        body.put("goal", dto.goal());
        body.put("goalLabel", dto.goalLabel());
        body.put("remaining", kilograms(dto.remaining()));
        body.put("percentage", dto.percentage());
        body.put("reached", dto.reached());
        body.put("trendPerWeek", kilograms(dto.trendPerWeek()));

        return body;
    }

    private static Map<String, Object> daySummary(DaySummaryDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("date", dto.date().toString());
        body.put("consumedCalories", dto.consumedCalories());
        body.put("consumedMacros", macros(dto.consumedMacros()));
        body.put("caloriePercentage", dto.caloriePercentage());
        body.put("withinRange", dto.withinRange());
        body.put("overBudget", dto.overBudget());

        return body;
    }

    private static Map<String, Object> macros(MacrosDto dto) {
        if (dto == null) {
            return null;
        }

        var body = new LinkedHashMap<String, Object>();
        body.put("protein", dto.protein());
        body.put("carbs", dto.carbs());
        body.put("fat", dto.fat());

        return body;
    }

    private static String kilograms(BigDecimal weight) {
        return weight.stripTrailingZeros().toPlainString();
    }
}
