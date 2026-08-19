package atlas.domain.routines.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PeriodWindowTest {

    private static final LocalDate START = LocalDate.of(2026, 2, 9);
    private static final LocalDate END = LocalDate.of(2026, 2, 16);

    @Test
    void shouldIncludeStartAndExcludeEnd() {
        var window = PeriodWindow.of(START, END);

        assertThat(window.contains(START)).isTrue();
        assertThat(window.contains(END.minusDays(1))).isTrue();
        assertThat(window.contains(END)).isFalse();
        assertThat(window.contains(START.minusDays(1))).isFalse();
    }

    @Test
    void shouldRejectWindowWhoseEndIsBeforeItsStart() {
        assertThatThrownBy(() -> PeriodWindow.of(END, START)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldRejectEmptyWindow() {
        assertThatThrownBy(() -> PeriodWindow.of(START, START)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldRejectNullBounds() {
        assertThatThrownBy(() -> PeriodWindow.of(null, END)).isInstanceOf(GuardException.class);
        assertThatThrownBy(() -> PeriodWindow.of(START, null)).isInstanceOf(GuardException.class);
    }
}
