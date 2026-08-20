package atlas.domain.economy.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.economy.vos.Money;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

class MovementKindTest {

    private static final Money TWELVE_FIFTY = Money.ofCents(1250).value();

    @Test
    void shouldLeaveIncomePositive() {
        assertThat(MovementKind.INCOME.signed(TWELVE_FIFTY)).isEqualTo(1250);
    }

    @Test
    void shouldMakeExpenseNegative() {
        assertThat(MovementKind.EXPENSE.signed(TWELVE_FIFTY)).isEqualTo(-1250);
    }

    @Test
    void shouldReadIncomeBackFromAPositiveAmount() {
        assertThat(MovementKind.of(1250)).isEqualTo(MovementKind.INCOME);
    }

    @Test
    void shouldReadExpenseBackFromANegativeAmount() {
        assertThat(MovementKind.of(-1250)).isEqualTo(MovementKind.EXPENSE);
    }

    @Test
    void shouldRejectAnAmountWithoutSign() {
        assertThatThrownBy(() -> MovementKind.of(0)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldRoundTripEveryKind() {
        for (var kind : MovementKind.values()) {
            assertThat(MovementKind.of(kind.signed(TWELVE_FIFTY))).isEqualTo(kind);
        }
    }
}
