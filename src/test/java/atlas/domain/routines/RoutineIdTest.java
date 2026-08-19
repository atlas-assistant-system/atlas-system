package atlas.domain.routines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sharedkernel.domain.exceptions.FormatException;
import sharedkernel.domain.exceptions.GuardException;

class RoutineIdTest {

    @Test
    void shouldRenderWithPrefixAndPadding() {
        assertThat(RoutineId.of(7)).hasToString("R00000007");
    }

    @Test
    void shouldParseBackToTheSameId() {
        assertThat(RoutineId.parse("R00000007")).isEqualTo(RoutineId.of(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A00000007", "R0000007", "R000000007", "RABCDEFGH", ""})
    void shouldRejectMalformedText(String text) {
        assertThatThrownBy(() -> RoutineId.parse(text)).isInstanceOf(FormatException.class);
        assertThat(RoutineId.tryParse(text)).isEmpty();
    }

    @Test
    void shouldRejectNegativeValues() {
        assertThatThrownBy(() -> RoutineId.of(-1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldRejectValuesBeyondItsLength() {
        assertThatThrownBy(() -> RoutineId.of(100_000_000L)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldWrapTheGivenUuidInAnEntryId() {
        var uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");

        assertThat(RoutineEntryId.of(uuid).value()).isEqualTo(uuid);
        assertThat(RoutineEntryId.of(uuid)).hasToString(uuid.toString());
    }
}
