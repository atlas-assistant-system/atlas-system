package atlas.domain.appointments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

class AppointmentIdTest {

    @Test
    void shouldRenderAsPrefixedTextWithLeadingZeros() {
        assertThat(AppointmentId.of(7)).hasToString("A00000007");
    }

    @Test
    void shouldParseItsOwnTextForm() {
        assertThat(AppointmentId.parse("A00000007")).isEqualTo(AppointmentId.of(7));
    }

    @Test
    void shouldRejectNegativeValues() {
        assertThatThrownBy(() -> AppointmentId.of(-1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldFailToParseTextWithTheWrongPrefix() {
        assertThatThrownBy(() -> AppointmentId.parse("U00000007")).isInstanceOf(FormatException.class);
    }

    @Test
    void shouldReturnEmptyWhenTextCannotBeParsed() {
        assertThat(AppointmentId.tryParse("nope")).isEmpty();
    }

    @Test
    void shouldReturnIdWhenTextCanBeParsed() {
        assertThat(AppointmentId.tryParse("A00000007")).contains(AppointmentId.of(7));
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(AppointmentId.of(42)).isEqualTo(AppointmentId.of(42));
    }
}
