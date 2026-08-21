package atlas.domain.nutrition.vos;

import atlas.domain.nutrition.NutritionErrors;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;
import atlas.domain.sharedkernel.results.Result;

public record Macros(int protein, int carbs, int fat) implements ValueObject {

    public static final Macros NONE = new Macros(0, 0, 0);

    private static final int KCAL_PER_PROTEIN_GRAM = 4;
    private static final int KCAL_PER_CARB_GRAM = 4;
    private static final int KCAL_PER_FAT_GRAM = 9;

    public Macros {
        NumberGuard.notNegative(protein, "protein");
        NumberGuard.notNegative(carbs, "carbs");
        NumberGuard.notNegative(fat, "fat");
    }

    public static Result<Macros> create(int protein, int carbs, int fat) {
        if (protein < 0 || carbs < 0 || fat < 0) {
            return Result.failure(NutritionErrors.MACROS_MUST_NOT_BE_NEGATIVE);
        }

        return Result.success(new Macros(protein, carbs, fat));
    }

    public Macros plus(Macros other) {
        return new Macros(protein + other.protein, carbs + other.carbs, fat + other.fat);
    }

    public Calories calories() {
        return new Calories(
            protein * KCAL_PER_PROTEIN_GRAM + carbs * KCAL_PER_CARB_GRAM + fat * KCAL_PER_FAT_GRAM);
    }

    public boolean isZero() {
        return protein == 0 && carbs == 0 && fat == 0;
    }
}
