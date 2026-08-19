package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

class SessionIdTest {

    @Test
    void shouldRenderAsPrefixedTextWithLeadingZeros() {
        assertThat(SessionId.of(7)).hasToString("S00000007");
    }

    @Test
    void shouldParseItsOwnTextForm() {
        assertThat(SessionId.parse("S00000007")).isEqualTo(SessionId.of(7));
    }

    @Test
    void shouldRejectNegativeValues() {
        assertThatThrownBy(() -> SessionId.of(-1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldFailToParseTextWithTheWrongPrefix() {
        assertThatThrownBy(() -> SessionId.parse("L00000007")).isInstanceOf(FormatException.class);
    }

    @Test
    void shouldReturnEmptyWhenTextCannotBeParsed() {
        assertThat(SessionId.tryParse("nope")).isEmpty();
    }

    @Test
    void shouldReturnIdWhenTextCanBeParsed() {
        assertThat(SessionId.tryParse("S00000007")).contains(SessionId.of(7));
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(SessionId.of(42)).isEqualTo(SessionId.of(42));
    }
}
