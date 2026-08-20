package atlas.domain.economy.vos;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.economy.MovementErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MovementNoteTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldLeaveTheNoteEmptyWhenNothingIsWritten(String text) {
        assertThat(MovementNote.create(text).value()).isEmpty();
    }

    @Test
    void shouldTrimSurroundingWhitespace() {
        assertThat(MovementNote.create("  cena con Ana  ").value())
            .map(MovementNote::value)
            .contains("cena con Ana");
    }

    @Test
    void shouldFailWhenTheNoteIsTooLong() {
        var result = MovementNote.create("x".repeat(MovementNote.MAX_LENGTH + 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(MovementErrors.NOTE_TOO_LONG);
    }

    @Test
    void shouldAcceptANoteOfExactlyTheMaximumLength() {
        assertThat(MovementNote.create("x".repeat(MovementNote.MAX_LENGTH)).value()).isPresent();
    }
}
