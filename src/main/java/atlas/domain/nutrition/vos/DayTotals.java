package atlas.domain.nutrition.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;

public record DayTotals(Macros consumed, Macros target) implements ValueObject {

    public DayTotals {
        ObjectGuard.notNull(consumed, "consumed");
        ObjectGuard.notNull(target, "target");
    }

    public int remainingProtein() {
        return target.protein() - consumed.protein();
    }

    public int remainingCarbs() {
        return target.carbs() - consumed.carbs();
    }

    public int remainingFat() {
        return target.fat() - consumed.fat();
    }

    public Calories consumedCalories() {
        return consumed.calories();
    }

    public Calories targetCalories() {
        return target.calories();
    }

    public int remainingCalories() {
        return consumedCalories().remainingFor(targetCalories());
    }

    public int caloriePercentage() {
        return consumedCalories().percentageOf(targetCalories());
    }

    public Calories lowerTarget() {
        return targetCalories().lowerBound();
    }

    public Calories upperTarget() {
        return targetCalories().upperBound();
    }

    public boolean isWithinRange() {
        return targetCalories().covers(consumedCalories());
    }

    public boolean isOverBudget() {
        return remainingCalories() < 0;
    }
}
