package sharedkernel.domain.guards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class NumberGuardTest {

    @Test
    void shouldReturnValueWhenZeroOrPositive() {
        assertThat(NumberGuard.notNegative(0L, "amount")).isZero();
        assertThat(NumberGuard.notNegative(7, "amount")).isEqualTo(7);
    }

    @Test
    void shouldThrowWhenNegative() {
        assertThatThrownBy(() -> NumberGuard.notNegative(-1L, "amount"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot be negative");
    }

    @Test
    void shouldThrowWhenZeroAndPositiveIsRequired() {
        assertThatThrownBy(() -> NumberGuard.positive(0, "quantity"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("must be greater than zero");
    }

    @Test
    void shouldAcceptBoundsWhenInRange() {
        assertThatCode(() -> NumberGuard.inRange(1, 1, 10, "page")).doesNotThrowAnyException();
        assertThatCode(() -> NumberGuard.inRange(10, 1, 10, "page")).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenOutOfRange() {
        assertThatThrownBy(() -> NumberGuard.inRange(11, 1, 10, "page"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("must be between 1 and 10");
    }
}
