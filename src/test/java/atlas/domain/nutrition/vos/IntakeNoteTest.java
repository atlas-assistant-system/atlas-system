package atlas.domain.nutrition.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.nutrition.IntakeErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IntakeNoteTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldBeAbsentWhenNoNoteWasWritten(String value) {
        var result = IntakeNote.create(value);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEmpty();
    }

    @Test
    void shouldTrimTheNote() {
        assertThat(IntakeNote.create("  Tortilla y pan  ").value())
            .contains(new IntakeNote("Tortilla y pan"));
    }

    @Test
    void shouldFailWhenTheNoteIsTooLong() {
        var result = IntakeNote.create("x".repeat(IntakeNote.MAX_LENGTH + 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(IntakeErrors.NOTE_TOO_LONG);
    }

    @Test
    void shouldAcceptANoteAtTheLimit() {
        var note = "x".repeat(IntakeNote.MAX_LENGTH);

        assertThat(IntakeNote.create(note).value()).contains(new IntakeNote(note));
    }

    @Test
    void shouldRejectABlankNoteBuiltDirectly() {
        assertThatThrownBy(() -> new IntakeNote(" ")).isInstanceOf(GuardException.class);
    }
}
