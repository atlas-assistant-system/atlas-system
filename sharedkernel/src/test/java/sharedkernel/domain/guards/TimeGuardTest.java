package sharedkernel.domain.guards;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class TimeGuardTest {

    private static final Instant NOW = Instant.parse("2026-08-16T10:15:30Z");

    @Test
    void shouldAcceptValueWhenItEqualsNow() {
        assertThatCode(() -> TimeGuard.notInFuture(NOW, NOW, "recordedAt")).doesNotThrowAnyException();
        assertThatCode(() -> TimeGuard.notInPast(NOW, NOW, "startsAt")).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenValueIsAfterNow() {
        assertThatThrownBy(() -> TimeGuard.notInFuture(NOW.plusSeconds(1), NOW, "recordedAt"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot be in the future");
    }

    @Test
    void shouldThrowWhenValueIsBeforeNow() {
        assertThatThrownBy(() -> TimeGuard.notInPast(NOW.minusSeconds(1), NOW, "startsAt"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot be in the past");
    }
}
