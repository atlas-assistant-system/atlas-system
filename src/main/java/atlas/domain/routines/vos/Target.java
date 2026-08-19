package atlas.domain.routines.vos;

import atlas.domain.routines.RoutineErrors;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.util.Optional;

public record Target(BigDecimal amount, Optional<Unit> unit) implements ValueObject {

    public Target {
        ObjectGuard.notNull(amount, "amount");
        ObjectGuard.notNull(unit, "unit");
    }

    public static Result<Target> create(BigDecimal amount, Unit unit) {
        if (amount == null || amount.signum() <= 0) {
            return Result.failure(RoutineErrors.TARGET_MUST_BE_POSITIVE);
        }

        return Result.success(new Target(normalize(amount), Optional.ofNullable(unit)));
    }

    public static Result<Target> create(BigDecimal amount) {
        return create(amount, null);
    }

    public static BigDecimal normalize(BigDecimal amount) {
        var stripped = amount.stripTrailingZeros();

        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    public boolean isMetBy(BigDecimal logged) {
        return logged.compareTo(amount) >= 0;
    }
}
