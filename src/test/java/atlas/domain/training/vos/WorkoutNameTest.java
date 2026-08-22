package atlas.domain.training.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.training.WorkoutErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class WorkoutNameTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldFailWhenThereIsNoNameToRead(String text) {
        var result = WorkoutName.create(text);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(WorkoutErrors.NAME_REQUIRED);
    }

    @Test
    void shouldFailWhenTheNameIsLongerThanTheLimit() {
        var result = WorkoutName.create("a".repeat(WorkoutName.MAX_LENGTH + 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(WorkoutErrors.NAME_TOO_LONG);
    }

    @Test
    void shouldAcceptExactlyTheLimit() {
        assertThat(WorkoutName.create("a".repeat(WorkoutName.MAX_LENGTH)).isSuccess()).isTrue();
    }

    @Test
    void shouldTrimWhatWasTyped() {
        assertThat(WorkoutName.create("  Dia de empuje  ").value().value()).isEqualTo("Dia de empuje");
    }

    @Test
    void shouldRejectABlankNameBuiltDirectly() {
        assertThatThrownBy(() -> new WorkoutName(" ")).isInstanceOf(GuardException.class);
    }
}
