package atlas.domain.nutrition.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Calories(int kcal) implements ValueObject {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Calories {
        NumberGuard.notNegative(kcal, "kcal");
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
}
