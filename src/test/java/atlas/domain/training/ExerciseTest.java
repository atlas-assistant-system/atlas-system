package atlas.domain.training;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.training.enums.Metric;
import atlas.domain.training.events.ExerciseArchivedEvent;
import atlas.domain.training.events.ExerciseDefinedEvent;
import atlas.domain.training.events.ExerciseRenamedEvent;
import atlas.domain.training.events.ExerciseUnarchivedEvent;
import atlas.domain.training.vos.ExerciseName;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExerciseTest {

    private static final ExerciseId ID = ExerciseId.of(1);
    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");

    @Test
    void shouldDefineAnExerciseWithItsMetric() {
        var exercise = define(Metric.LOAD);

        assertThat(exercise.name().value()).isEqualTo("Press banca");
        assertThat(exercise.metric()).isEqualTo(Metric.LOAD);
        assertThat(exercise.isArchived()).isFalse();
    }

    @Test
    void shouldRaiseDefinedEventWhenAnExerciseIsCreated() {
        assertThat(define(Metric.LOAD).pendingEvents())
            .containsExactly(new ExerciseDefinedEvent(ID, NOW));
    }

    @Test
    void shouldRenameAndSaySo() {
        var exercise = define(Metric.LOAD);
        exercise.clearEvents();

        var result = exercise.rename(new ExerciseName("Press inclinado"), NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(exercise.name().value()).isEqualTo("Press inclinado");
        assertThat(exercise.pendingEvents()).containsExactly(new ExerciseRenamedEvent(ID, NOW));
    }

    @Test
    void shouldArchiveAndSaySo() {
        var exercise = define(Metric.LOAD);
        exercise.clearEvents();

        var result = exercise.archive(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(exercise.isArchived()).isTrue();
        assertThat(exercise.pendingEvents()).containsExactly(new ExerciseArchivedEvent(ID, NOW));
    }

    @Test
    void shouldNotArchiveTwice() {
        var exercise = define(Metric.LOAD);
        exercise.archive(NOW);

        var result = exercise.archive(NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(ExerciseErrors.ALREADY_ARCHIVED);
    }

    @Test
    void shouldNotRenameAnArchivedExercise() {
        var exercise = define(Metric.LOAD);
        exercise.archive(NOW);

        var result = exercise.rename(new ExerciseName("Otro"), NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(ExerciseErrors.ALREADY_ARCHIVED);
    }

    @Test
    void shouldBringAnArchivedExerciseBack() {
        var exercise = define(Metric.LOAD);
        exercise.archive(NOW);
        exercise.clearEvents();

        var result = exercise.unarchive(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(exercise.isArchived()).isFalse();
        assertThat(exercise.pendingEvents()).containsExactly(new ExerciseUnarchivedEvent(ID, NOW));
    }

    @Test
    void shouldNotUnarchiveSomethingThatWasNeverArchived() {
        var result = define(Metric.LOAD).unarchive(NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(ExerciseErrors.NOT_ARCHIVED);
    }

    @Test
    void shouldRehydrateWithoutRaisingAnyEvent() {
        var exercise = Exercise.rehydrate(ID, new ExerciseName("Plancha"), Metric.TIME, true);

        assertThat(exercise.metric()).isEqualTo(Metric.TIME);
        assertThat(exercise.isArchived()).isTrue();
        assertThat(exercise.pendingEvents()).isEmpty();
    }

    private static Exercise define(Metric metric) {
        return Exercise.define(ID, new ExerciseName("Press banca"), metric, NOW).value();
    }
}
