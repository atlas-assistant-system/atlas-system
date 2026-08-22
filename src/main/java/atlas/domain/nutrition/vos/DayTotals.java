package atlas.domain.nutrition.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;

public record DayTotals(
    Macros consumedMacros,
    Calories consumedCalories,
    Macros targetMacros,
    Calories targetCalories) implements ValueObject {

    public DayTotals {
        ObjectGuard.notNull(consumedMacros, "consumedMacros");
        ObjectGuard.notNull(consumedCalories, "consumedCalories");
        ObjectGuard.notNull(targetMacros, "targetMacros");
        ObjectGuard.notNull(targetCalories, "targetCalories");
    }

    public int remainingProtein() {
        return targetMacros.protein() - consumedMacros.protein();
    }

    public int remainingCarbs() {
        return targetMacros.carbs() - consumedMacros.carbs();
    }

    public int remainingFat() {
        return targetMacros.fat() - consumedMacros.fat();
    }

    public int remainingCalories() {
        return consumedCalories.remainingFor(targetCalories);
    }

    public int caloriePercentage() {
        return consumedCalories.percentageOf(targetCalories);
    }

    public Calories lowerTarget() {
        return targetCalories.lowerBound();
    }

    public Calories upperTarget() {
        return targetCalories.upperBound();
    }

    public boolean isWithinRange() {
        return targetCalories.covers(consumedCalories);
    }

    public boolean isOverBudget() {
        return remainingCalories() < 0;
    }
}
