package atlas.application.nutrition.mappers;

import atlas.application.nutrition.dto.DayDto;
import atlas.application.nutrition.dto.DaySummaryDto;
import atlas.application.nutrition.dto.IntakeDto;
import atlas.application.nutrition.dto.MacrosDto;
import atlas.application.nutrition.dto.PlanDto;
import atlas.application.nutrition.dto.ProgressDto;
import atlas.application.nutrition.dto.WeighInDto;
import atlas.application.nutrition.ports.DayConsumption;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.Plan;
import atlas.domain.nutrition.WeighIn;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.DayTotals;
import atlas.domain.nutrition.vos.IntakeNote;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.nutrition.vos.Progress;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class NutritionMapper {

    private static final int GRAM_SCALE = 3;

    private NutritionMapper() {}

    public static PlanDto toDto(Plan plan) {
        var goal = plan.goal();

        return new PlanDto(
            plan.id().toString(),
            plan.startWeight().toKilograms(),
            plan.targetWeight().toKilograms(),
            goal.name(),
            goal.label(),
            plan.dailyCalories().kcal(),
            toDto(plan.dailyMacros()),
            plan.status().name(),
            plan.startedOn(),
            plan.definedAt());
    }

    public static IntakeDto toDto(Intake intake) {
        return new IntakeDto(
            intake.id().toString(),
            intake.calories().kcal(),
            toDto(intake.macros()),
            intake.note().map(IntakeNote::value).orElse(null),
            intake.consumedOn(),
            intake.recordedAt());
    }

    public static List<IntakeDto> toDtos(Collection<Intake> intakes) {
        return intakes.stream().map(NutritionMapper::toDto).toList();
    }

    public static WeighInDto toDto(WeighIn weighIn) {
        return new WeighInDto(
            weighIn.id().toString(),
            weighIn.weight().toKilograms(),
            weighIn.measuredOn(),
            weighIn.recordedAt());
    }

    public static List<WeighInDto> toWeighInDtos(Collection<WeighIn> weighIns) {
        return weighIns.stream()
            .sorted(Comparator.comparing(WeighIn::measuredOn))
            .map(NutritionMapper::toDto)
            .toList();
    }

    public static ProgressDto toDto(Progress progress) {
        return new ProgressDto(
            progress.start().toKilograms(),
            progress.current().toKilograms(),
            progress.target().toKilograms(),
            progress.goal().name(),
            progress.goal().label(),
            kilograms(progress.remainingGrams()),
            progress.percentage(),
            progress.reached(),
            kilograms(progress.trendGramsPerWeek()));
    }

    public static MacrosDto toDto(Macros macros) {
        return new MacrosDto(macros.protein(), macros.carbs(), macros.fat());
    }

    public static DayDto toDto(LocalDate date, DayTotals totals, Collection<Intake> intakes) {
        return new DayDto(
            date,
            totals.consumedCalories().kcal(),
            toDto(totals.consumedMacros()),
            totals.targetCalories().kcal(),
            toDto(totals.targetMacros()),
            totals.remainingCalories(),
            remainingOf(totals),
            totals.caloriePercentage(),
            totals.lowerTarget().kcal(),
            totals.upperTarget().kcal(),
            totals.isWithinRange(),
            totals.isOverBudget(),
            toDtos(intakes));
    }

    public static DayDto toDto(
        LocalDate date, Calories consumed, Macros macros, Collection<Intake> intakes) {

        return new DayDto(
            date, consumed.kcal(), toDto(macros), null, null, null, null,
            0, 0, 0, false, false, toDtos(intakes));
    }

    public static List<DaySummaryDto> toDays(Collection<DayConsumption> consumption, Calories dailyCalories) {
        return consumption.stream()
            .sorted(Comparator.comparing(DayConsumption::date).reversed())
            .map(day -> toSummaryDto(day, dailyCalories))
            .toList();
    }

    private static DaySummaryDto toSummaryDto(DayConsumption day, Calories dailyCalories) {
        var consumed = day.calories();

        return new DaySummaryDto(
            day.date(),
            consumed.kcal(),
            toDto(day.macros()),
            consumed.percentageOf(dailyCalories),
            dailyCalories.covers(consumed),
            dailyCalories.kcal() > 0 && consumed.remainingFor(dailyCalories) < 0);
    }

    private static MacrosDto remainingOf(DayTotals totals) {
        return new MacrosDto(
            totals.remainingProtein(), totals.remainingCarbs(), totals.remainingFat());
    }

    private static BigDecimal kilograms(int grams) {
        return BigDecimal.valueOf(grams, GRAM_SCALE);
    }
}
