package atlas.application.training.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.WorkoutLogId;
import atlas.domain.training.entities.PlannedExercise;
import atlas.domain.training.entities.PlannedExerciseId;
import atlas.domain.training.entities.SetLog;
import atlas.domain.training.entities.SetLogId;
import atlas.domain.training.enums.Metric;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.ExerciseName;
import atlas.domain.training.vos.SetCount;
import atlas.domain.training.vos.WorkoutName;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrainingMapperTest {

    private static final UUID UUID_ONE = new UUID(0, 1);
    private static final ExerciseId PRESS = ExerciseId.of(10);
    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);

    @Test
    void shouldRenderAnExerciseWithItsMetricAndLabel() {
        var exercise = Exercise.rehydrate(PRESS, new ExerciseName("Press banca"), Metric.LOAD, false);

        var dto = TrainingMapper.toDto(exercise);

        assertThat(dto.id()).isEqualTo("E00000010");
        assertThat(dto.name()).isEqualTo("Press banca");
        assertThat(dto.metric()).isEqualTo("LOAD");
        assertThat(dto.metricLabel()).isEqualTo("Carga");
        assertThat(dto.archived()).isFalse();
    }

    @Test
    void shouldTurnGramsBackIntoKilograms() {
        var dto = TrainingMapper.toDto(new Effort(72_500, 8, 0, 0));

        assertThat(dto.load()).isEqualByComparingTo("72.5");
        assertThat(dto.reps()).isEqualTo(8);
        assertThat(dto.seconds()).isZero();
        assertThat(dto.meters()).isZero();
    }

    @Test
    void shouldRenderAPlanLineWithItsPositionAndSets() {
        var line = new PlannedExercise(
            PlannedExerciseId.of(UUID_ONE), PRESS, 0, new SetCount(4), new Effort(70_000, 8, 0, 0));
        var workout = Workout.rehydrate(
            WorkoutId.of(5), new WorkoutName("Empuje"), List.of(line),
            Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), false);

        var dto = TrainingMapper.toDto(workout);

        assertThat(dto.id()).isEqualTo("W00000005");
        assertThat(dto.name()).isEqualTo("Empuje");
        assertThat(dto.days()).containsExactly("MONDAY", "THURSDAY");
        assertThat(dto.plan()).hasSize(1);
        assertThat(dto.plan().getFirst().exerciseId()).isEqualTo("E00000010");
        assertThat(dto.plan().getFirst().sets()).isEqualTo(4);
        assertThat(dto.plan().getFirst().position()).isZero();
        assertThat(dto.plan().getFirst().target().load()).isEqualByComparingTo("70");
    }

    @Test
    void shouldLeaveThePlannedEffortOutWhenTheSetWasNotInTheScript() {
        var set = new SetLog(
            SetLogId.of(UUID_ONE), PRESS, 0, Optional.empty(), Optional.of(new Effort(70_000, 6, 0, 0)));

        var dto = TrainingMapper.toDto(set);

        assertThat(dto.planned()).isNull();
        assertThat(dto.actual().reps()).isEqualTo(6);
    }

    @Test
    void shouldLeaveTheActualEffortOutWhileTheSetIsStillPending() {
        var set = new SetLog(
            SetLogId.of(UUID_ONE), PRESS, 0, Optional.of(new Effort(70_000, 8, 0, 0)), Optional.empty());

        var dto = TrainingMapper.toDto(set);

        assertThat(dto.planned().reps()).isEqualTo(8);
        assertThat(dto.actual()).isNull();
    }

    @Test
    void shouldRenderALogWithoutAWorkoutWhenItWasAFreeSession() {
        var log = WorkoutLog.rehydrate(
            WorkoutLogId.of(3), Optional.empty(), TODAY, NOW, List.of());

        var dto = TrainingMapper.toDto(log);

        assertThat(dto.id()).isEqualTo("T00000003");
        assertThat(dto.workoutId()).isNull();
        assertThat(dto.performedOn()).isEqualTo(TODAY);
        assertThat(dto.startedAt()).isEqualTo(NOW);
        assertThat(dto.sets()).isEmpty();
    }

    @Test
    void shouldRenderALogWithTheWorkoutItCameFrom() {
        var log = WorkoutLog.rehydrate(
            WorkoutLogId.of(3), Optional.of(WorkoutId.of(5)), TODAY, NOW, List.of());

        assertThat(TrainingMapper.toDto(log).workoutId()).isEqualTo("W00000005");
    }
}
