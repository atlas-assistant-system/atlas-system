package sharedkernel.domain.guards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import sharedkernel.domain.exceptions.GuardException;

class StringGuardTest {

    private static final Pattern SLUG = Pattern.compile("[a-z-]+");

    @Test
    void shouldReturnValueWhenNotBlank() {
        assertThat(StringGuard.notBlank("checkup", "title")).isEqualTo("checkup");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void shouldThrowWhenBlank(String value) {
        assertThatThrownBy(() -> StringGuard.notBlank(value, "title"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot be blank");
    }

    @Test
    void shouldAcceptValueWhenExactlyAtMaxLength() {
        assertThatCode(() -> StringGuard.notLongerThan("abc", 3, "title")).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenLongerThanMax() {
        assertThatThrownBy(() -> StringGuard.notLongerThan("abcd", 3, "title"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot exceed 3 characters");
    }

    @Test
    void shouldReturnValueWhenPatternMatches() {
        assertThat(StringGuard.matches("time-slot", SLUG, "slug")).isEqualTo("time-slot");
    }

    @Test
    void shouldThrowWhenPatternDoesNotMatch() {
        assertThatThrownBy(() -> StringGuard.matches("Time Slot", SLUG, "slug"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("does not match");
    }
}
