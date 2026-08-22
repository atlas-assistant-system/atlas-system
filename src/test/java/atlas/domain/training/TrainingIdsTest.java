package atlas.domain.training;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.training.entities.PlannedExerciseId;
import atlas.domain.training.entities.SetLogId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TrainingIdsTest {

    @Test
    void shouldRenderAnExerciseWithItsOwnPrefix() {
        assertThat(ExerciseId.of(7)).hasToString("E00000007");
    }

    @Test
    void shouldRenderAWorkoutWithItsOwnPrefix() {
        assertThat(WorkoutId.of(7)).hasToString("W00000007");
    }

    @Test
    void shouldRenderAWorkoutLogWithItsOwnPrefix() {
        assertThat(WorkoutLogId.of(7)).hasToString("T00000007");
    }

    @Test
    void shouldParseBackTheValueEachIdRendered() {
        assertThat(ExerciseId.parse("E00000042")).isEqualTo(ExerciseId.of(42));
        assertThat(WorkoutId.parse("W00000042")).isEqualTo(WorkoutId.of(42));
        assertThat(WorkoutLogId.parse("T00000042")).isEqualTo(WorkoutLogId.of(42));
    }

    @Test
    void shouldTryParseIntoTheSameId() {
        assertThat(ExerciseId.tryParse("E00000007")).contains(ExerciseId.of(7));
        assertThat(WorkoutId.tryParse("W00000007")).contains(WorkoutId.of(7));
        assertThat(WorkoutLogId.tryParse("T00000007")).contains(WorkoutLogId.of(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"W00000007", "E7", "E000000AA", ""})
    void shouldNotParseSomethingThatIsNotAnExerciseId(String text) {
        assertThat(ExerciseId.tryParse(text)).isEmpty();
    }

    @Test
    void shouldNotConfuseTheThreePrefixesWithEachOther() {
        assertThat(ExerciseId.tryParse("T00000007")).isEmpty();
        assertThat(WorkoutId.tryParse("E00000007")).isEmpty();
        assertThat(WorkoutLogId.tryParse("W00000007")).isEmpty();
    }

    @Test
    void shouldCarryTheUuidOfAnInternalEntityId() {
        var uuid = UUID.fromString("0f1b6e4a-4d1e-4a3f-9c2b-8a7d6e5f4c3b");

        assertThat(PlannedExerciseId.of(uuid).value()).isEqualTo(uuid);
        assertThat(PlannedExerciseId.of(uuid)).hasToString(uuid.toString());
        assertThat(SetLogId.of(uuid)).hasToString(uuid.toString());
    }
}
