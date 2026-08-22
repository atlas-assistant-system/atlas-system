package atlas.domain.nutrition.vos;

import atlas.domain.nutrition.NutritionErrors;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Calories(int kcal) implements ValueObject {

    public static final Calories NONE = new Calories(0);
    public static final int TOLERANCE_PERCENTAGE = 10;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Calories {
        NumberGuard.notNegative(kcal, "kcal");
    }

    public static Result<Calories> create(int kcal) {
        if (kcal < 0) {
            return Result.failure(NutritionErrors.CALORIES_MUST_NOT_BE_NEGATIVE);
        }

        return Result.success(new Calories(kcal));
    }

    public Calories plus(Calories other) {
        return new Calories(kcal + other.kcal);
    }

    public boolean isZero() {
        return kcal == 0;
    }

    public int remainingFor(Calories target) {
        return target.kcal - kcal;
    }

    public int percentageOf(Calories total) {
        if (total.kcal == 0) {
            return 0;
        }

        return BigDecimal.valueOf(kcal)
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(total.kcal), 0, RoundingMode.HALF_UP)
            .intValueExact();
    }

    public Calories lowerBound() {
        return scaledBy(HUNDRED.subtract(BigDecimal.valueOf(TOLERANCE_PERCENTAGE)));
    }

    public Calories upperBound() {
        return scaledBy(HUNDRED.add(BigDecimal.valueOf(TOLERANCE_PERCENTAGE)));
    }

    public boolean covers(Calories consumed) {
        return kcal > 0
            && consumed.kcal >= lowerBound().kcal
            && consumed.kcal <= upperBound().kcal;
    }

    private Calories scaledBy(BigDecimal percentage) {
        return new Calories(BigDecimal.valueOf(kcal)
            .multiply(percentage)
            .divide(HUNDRED, 0, RoundingMode.HALF_UP)
            .intValueExact());
    }
}
