package sharedkernel.domain.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sharedkernel.domain.exceptions.FormatException;
import sharedkernel.domain.exceptions.GuardException;

class PrefixedIdsTest {

    @Test
    void shouldPadWithZerosWhenFormatting() {
        assertThat(PrefixedIds.format('A', 8, 7)).isEqualTo("A00000007");
    }

    @Test
    void shouldFormatMaximumValueWhenItFitsTheLength() {
        assertThat(PrefixedIds.format('A', 8, 99_999_999)).isEqualTo("A99999999");
    }

    @Test
    void shouldThrowWhenValueIsNegative() {
        assertThatThrownBy(() -> PrefixedIds.format('A', 8, -1))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenValueExceedsLength() {
        assertThatThrownBy(() -> PrefixedIds.format('A', 8, 100_000_000))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldRoundTripWhenParsingFormattedId() {
        var formatted = PrefixedIds.format('U', 8, 42);

        assertThat(PrefixedIds.parse('U', 8, formatted)).isEqualTo(42);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A0000007", "A000000007", "B00000007", "Axx000007", ""})
    void shouldThrowWhenParsingMalformedId(String malformed) {
        assertThatThrownBy(() -> PrefixedIds.parse('A', 8, malformed))
            .isInstanceOf(FormatException.class);
    }

    @Test
    void shouldThrowWhenParsingNull() {
        assertThatThrownBy(() -> PrefixedIds.parse('A', 8, null))
            .isInstanceOf(FormatException.class);
    }

    @Test
    void shouldReturnValueWhenTryParsingValidId() {
        assertThat(PrefixedIds.tryParse('A', 8, "A00000042")).contains(42L);
    }

    @Test
    void shouldReturnEmptyWhenTryParsingMalformedId() {
        assertThat(PrefixedIds.tryParse('A', 8, "garbage")).isEmpty();
    }
}
