package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

class AuthenticationGateIdTest {

    @Test
    void shouldRenderAsPrefixedTextWithLeadingZeros() {
        assertThat(AuthenticationGateId.of(7)).hasToString("G00000007");
    }

    @Test
    void shouldParseItsOwnTextForm() {
        assertThat(AuthenticationGateId.parse("G00000007")).isEqualTo(AuthenticationGateId.of(7));
    }

    @Test
    void shouldRejectNegativeValues() {
        assertThatThrownBy(() -> AuthenticationGateId.of(-1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldFailToParseTextWithTheWrongPrefix() {
        assertThatThrownBy(() -> AuthenticationGateId.parse("B00000007")).isInstanceOf(FormatException.class);
    }

    @Test
    void shouldReturnEmptyWhenTextCannotBeParsed() {
        assertThat(AuthenticationGateId.tryParse("nope")).isEmpty();
    }

    @Test
    void shouldReturnIdWhenTextCanBeParsed() {
        assertThat(AuthenticationGateId.tryParse("G00000007")).contains(AuthenticationGateId.of(7));
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(AuthenticationGateId.of(42)).isEqualTo(AuthenticationGateId.of(42));
    }

    @Test
    void shouldExposeTheDeviceSingletonId() {
        assertThat(AuthenticationGateId.single()).isEqualTo(AuthenticationGateId.of(1));
    }

    @Test
    void shouldRenderTheDeviceSingletonAsFixedText() {
        assertThat(AuthenticationGateId.single()).hasToString("G00000001");
    }
}
