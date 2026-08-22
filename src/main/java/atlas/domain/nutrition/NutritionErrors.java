package atlas.domain.nutrition;

import atlas.domain.sharedkernel.results.Error;

public final class NutritionErrors {

    public static final Error WEIGHT_OUT_OF_RANGE = Error.validation(
        "Nutrition.WeightOutOfRange", "The weight is outside the range a person can weigh.");

    public static final Error MACROS_MUST_NOT_BE_NEGATIVE =
        Error.validation("Nutrition.MacrosMustNotBeNegative", "Macros are grams, never below zero.");

    public static final Error CALORIES_MUST_NOT_BE_NEGATIVE =
        Error.validation("Nutrition.CaloriesMustNotBeNegative", "Calories are never below zero.");

    private NutritionErrors() {}
}
