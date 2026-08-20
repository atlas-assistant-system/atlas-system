package atlas.domain.economy.vos;

import atlas.domain.economy.MovementErrors;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.guards.NumberGuard;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(long cents, String currency) implements ValueObject {

    public static final String EUR = "EUR";

    private static final int CENT_SCALE = 2;

    public Money {
        NumberGuard.positive(cents, "cents");
        StringGuard.notBlank(currency, "currency");
    }

    public static Result<Money> ofCents(long cents) {
        if (cents <= 0) {
            return Result.failure(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
        }

        return Result.success(new Money(cents, EUR));
    }

    public static Result<Money> ofEuros(BigDecimal euros) {
        if (euros == null) {
            return Result.failure(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
        }

        return ofCents(euros.setScale(CENT_SCALE, RoundingMode.HALF_UP).unscaledValue().longValueExact());
    }

    public Money plus(Money other) {
        return new Money(cents + sameCurrencyAs(other), currency);
    }

    public Money minus(Money other) {
        return new Money(cents - sameCurrencyAs(other), currency);
    }

    public BigDecimal toEuros() {
        return BigDecimal.valueOf(cents, CENT_SCALE);
    }

    private long sameCurrencyAs(Money other) {
        if (!currency.equals(other.currency)) {
            throw GuardException.forParameter("other", "must be expressed in " + currency);
        }

        return other.cents;
    }
}
