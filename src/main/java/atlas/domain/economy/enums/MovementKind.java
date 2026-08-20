package atlas.domain.economy.enums;

import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.exceptions.GuardException;

public enum MovementKind {

    INCOME {

        @Override
        public long signed(Money amount) {
            return amount.cents();
        }
    },
    EXPENSE {

        @Override
        public long signed(Money amount) {
            return -amount.cents();
        }
    };

    public abstract long signed(Money amount);

    public static MovementKind of(long signedCents) {
        if (signedCents == 0) {
            throw GuardException.forParameter("signedCents", "cannot be zero");
        }

        return signedCents > 0 ? INCOME : EXPENSE;
    }
}
