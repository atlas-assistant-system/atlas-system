package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileNameTest {

    @Test
    void shouldCreateNameWhenTextIsPresent() {
        var result = ProfileName.create("Eduardo");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().value()).isEqualTo("Eduardo");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldFailWhenNameIsBlank(String candidate) {
        var result = ProfileName.create(candidate);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.PROFILE_NAME_REQUIRED);
    }

    @Test
    void shouldTrimSurroundingWhitespace() {
        var result = ProfileName.create("  Eduardo  ");

        assertThat(result.value().value()).isEqualTo("Eduardo");
    }

    @Test
    void shouldAcceptNameAtMaximumLength() {
        var result = ProfileName.create("a".repeat(ProfileName.MAX_LENGTH));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldFailWhenNameExceedsMaximumLength() {
        var result = ProfileName.create("a".repeat(ProfileName.MAX_LENGTH + 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.PROFILE_NAME_TOO_LONG);
    }

    @Test
    void shouldNotCountTrimmedWhitespaceTowardsTheLimit() {
        var result = ProfileName.create("  " + "a".repeat(ProfileName.MAX_LENGTH) + "  ");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldThrowWhenInternallyBuiltNameIsBlank() {
        assertThatThrownBy(() -> ProfileName.of("   ")).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldKeepItsValueWhenInternallyBuilt() {
        assertThat(ProfileName.of("Eduardo").value()).isEqualTo("Eduardo");
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(ProfileName.of("Eduardo")).isEqualTo(ProfileName.of("Eduardo"));
    }

    @Test
    void shouldNotBeEqualWhenValuesDiffer() {
        assertThat(ProfileName.of("Eduardo")).isNotEqualTo(ProfileName.of("Marta"));
    }

    @Test
    void shouldPrintItsValue() {
        assertThat(ProfileName.of("Eduardo")).hasToString("Eduardo");
    }
}
