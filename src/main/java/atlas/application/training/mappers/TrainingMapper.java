package atlas.application.training.mappers;

import atlas.application.training.dto.EffortDto;
import atlas.application.training.dto.ExerciseDto;
import atlas.application.training.dto.PlannedExerciseDto;
import atlas.application.training.dto.SetLogDto;
import atlas.application.training.dto.WorkoutDto;
import atlas.application.training.dto.WorkoutLogDto;
import atlas.domain.training.Exercise;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.entities.PlannedExercise;
import atlas.domain.training.entities.SetLog;
import atlas.domain.training.vos.Effort;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;

public final class TrainingMapper {

    private TrainingMapper() {}

    public static ExerciseDto toDto(Exercise exercise) {
        var metric = exercise.metric();

        return new ExerciseDto(
            exercise.id().toString(),
            exercise.name().value(),
            metric.name(),
            metric.label(),
            exercise.isArchived());
    }

    public static EffortDto toDto(Effort effort) {
        return new EffortDto(
            effort.loadKilograms(), effort.reps(), effort.seconds(), effort.meters());
    }

    public static WorkoutDto toDto(Workout workout) {
        return new WorkoutDto(
            workout.id().toString(),
            workout.name().value(),
            workout.days().stream().map(DayOfWeek::name).toList(),
            workout.isArchived(),
            workout.plan().stream().map(TrainingMapper::toDto).toList());
    }

    public static PlannedExerciseDto toDto(PlannedExercise line) {
        return new PlannedExerciseDto(
            line.id().toString(),
            line.exerciseId().toString(),
            line.position(),
            line.sets().value(),
            toDto(line.target()));
    }

    public static SetLogDto toDto(SetLog set) {
        return new SetLogDto(
            set.id().toString(),
            set.exerciseId().toString(),
            set.position(),
            set.planned().map(TrainingMapper::toDto).orElse(null),
            set.actual().map(TrainingMapper::toDto).orElse(null));
    }

    public static WorkoutLogDto toDto(WorkoutLog log) {
        return new WorkoutLogDto(
            log.id().toString(),
            log.workoutId().map(WorkoutId::toString).orElse(null),
            log.performedOn(),
            log.startedAt(),
            log.sets().stream().map(TrainingMapper::toDto).toList());
    }

    public static List<ExerciseDto> toExercises(Collection<Exercise> exercises) {
        return exercises.stream().map(TrainingMapper::toDto).toList();
    }

    public static List<WorkoutDto> toWorkouts(Collection<Workout> workouts) {
        return workouts.stream().map(TrainingMapper::toDto).toList();
    }

    public static List<WorkoutLogDto> toLogs(Collection<WorkoutLog> logs) {
        return logs.stream().map(TrainingMapper::toDto).toList();
    }
}
