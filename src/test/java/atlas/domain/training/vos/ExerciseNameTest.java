package atlas.domain.training.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.training.ExerciseErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ExerciseNameTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldFailWhenThereIsNoNameToRead(String text) {
        var result = ExerciseName.create(text);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(ExerciseErrors.NAME_REQUIRED);
    }

    @Test
    void shouldFailWhenTheNameIsLongerThanTheLimit() {
        var result = ExerciseName.create("a".repeat(ExerciseName.MAX_LENGTH + 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(ExerciseErrors.NAME_TOO_LONG);
    }

    @Test
    void shouldAcceptExactlyTheLimit() {
        assertThat(ExerciseName.create("a".repeat(ExerciseName.MAX_LENGTH)).isSuccess()).isTrue();
    }

    @Test
    void shouldTrimWhatWasTypedSoTwoNamesDoNotDifferByASpace() {
        assertThat(ExerciseName.create("  Press banca  ").value().value()).isEqualTo("Press banca");
    }

    @Test
    void shouldRejectABlankNameBuiltDirectly() {
        assertThatThrownBy(() -> new ExerciseName(" ")).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(ExerciseName.create("Press banca").value()).isEqualTo(new ExerciseName("Press banca"));
    }
}
