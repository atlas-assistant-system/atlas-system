package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

class BiometricProfileIdTest {

    @Test
    void shouldRenderAsPrefixedTextWithLeadingZeros() {
        assertThat(BiometricProfileId.of(7)).hasToString("B00000007");
    }

    @Test
    void shouldParseItsOwnTextForm() {
        assertThat(BiometricProfileId.parse("B00000007")).isEqualTo(BiometricProfileId.of(7));
    }

    @Test
    void shouldRejectNegativeValues() {
        assertThatThrownBy(() -> BiometricProfileId.of(-1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldFailToParseTextWithTheWrongPrefix() {
        assertThatThrownBy(() -> BiometricProfileId.parse("A00000007")).isInstanceOf(FormatException.class);
    }

    @Test
    void shouldReturnEmptyWhenTextCannotBeParsed() {
        assertThat(BiometricProfileId.tryParse("nope")).isEmpty();
    }

    @Test
    void shouldReturnIdWhenTextCanBeParsed() {
        assertThat(BiometricProfileId.tryParse("B00000007")).contains(BiometricProfileId.of(7));
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(BiometricProfileId.of(42)).isEqualTo(BiometricProfileId.of(42));
    }
}
