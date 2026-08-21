package atlas.domain.nutrition.vos;

import atlas.domain.nutrition.NutritionErrors;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Weight(int grams) implements ValueObject {

    public static final int MIN_GRAMS = 20_000;
    public static final int MAX_GRAMS = 400_000;

    private static final int GRAM_SCALE = 3;

    public Weight {
        NumberGuard.inRange(grams, MIN_GRAMS, MAX_GRAMS, "grams");
    }

    public static Result<Weight> ofGrams(int grams) {
        if (grams < MIN_GRAMS || grams > MAX_GRAMS) {
            return Result.failure(NutritionErrors.WEIGHT_OUT_OF_RANGE);
        }

        return Result.success(new Weight(grams));
    }

    public static Result<Weight> ofKilograms(BigDecimal kilograms) {
        if (kilograms == null) {
            return Result.failure(NutritionErrors.WEIGHT_OUT_OF_RANGE);
        }

        var grams = kilograms.setScale(GRAM_SCALE, RoundingMode.HALF_UP).unscaledValue();
        if (grams.bitLength() >= Integer.SIZE) {
            return Result.failure(NutritionErrors.WEIGHT_OUT_OF_RANGE);
        }

        return ofGrams(grams.intValueExact());
    }

    public int gramsTo(Weight other) {
        return other.grams - grams;
    }

    public BigDecimal toKilograms() {
        return BigDecimal.valueOf(grams, GRAM_SCALE);
    }
}
