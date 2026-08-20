package atlas.domain.economy.vos;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.economy.SavingsGoalErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GoalNameTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldFailWhenThereIsNoName(String text) {
        var result = GoalName.create(text);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(SavingsGoalErrors.NAME_REQUIRED);
    }

    @Test
    void shouldTrimSurroundingWhitespace() {
        assertThat(GoalName.create("  Viaje a Japon  ").value().value()).isEqualTo("Viaje a Japon");
    }

    @Test
    void shouldFailWhenTheNameIsTooLong() {
        var result = GoalName.create("x".repeat(GoalName.MAX_LENGTH + 1));

        assertThat(result.error()).isEqualTo(SavingsGoalErrors.NAME_TOO_LONG);
    }

    @Test
    void shouldAcceptANameOfExactlyTheMaximumLength() {
        assertThat(GoalName.create("x".repeat(GoalName.MAX_LENGTH)).isSuccess()).isTrue();
    }
}
