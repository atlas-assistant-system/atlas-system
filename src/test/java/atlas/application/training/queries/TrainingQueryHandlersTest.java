package atlas.application.training.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.training.ports.ExerciseReadModel;
import atlas.application.training.ports.WorkoutLogReadModel;
import atlas.application.training.ports.WorkoutReadModel;
import atlas.application.training.queries.gettodayworkout.GetTodayWorkoutQuery;
import atlas.application.training.queries.gettodayworkout.GetTodayWorkoutQueryHandler;
import atlas.application.training.queries.getworkout.GetWorkoutQuery;
import atlas.application.training.queries.getworkout.GetWorkoutQueryHandler;
import atlas.application.training.queries.getworkoutlog.GetWorkoutLogQuery;
import atlas.application.training.queries.getworkoutlog.GetWorkoutLogQueryHandler;
import atlas.application.training.queries.listexercises.ListExercisesQuery;
import atlas.application.training.queries.listexercises.ListExercisesQueryHandler;
import atlas.application.training.queries.listworkoutlogs.ListWorkoutLogsQuery;
import atlas.application.training.queries.listworkoutlogs.ListWorkoutLogsQueryHandler;
import atlas.application.training.queries.listworkouts.ListWorkoutsQuery;
import atlas.application.training.queries.listworkouts.ListWorkoutsQueryHandler;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutErrors;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.WorkoutLogErrors;
import atlas.domain.training.WorkoutLogId;
import atlas.domain.training.enums.Metric;
import atlas.domain.training.vos.ExerciseName;
import atlas.domain.training.vos.WorkoutName;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TrainingQueryHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final WorkoutId WORKOUT = WorkoutId.of(5);
    private static final WorkoutLogId LOG = WorkoutLogId.of(3);

    private final ExerciseReadModel exercises = mock(ExerciseReadModel.class);
    private final WorkoutReadModel workouts = mock(WorkoutReadModel.class);
    private final WorkoutLogReadModel logs = mock(WorkoutLogReadModel.class);

    @Test
    void shouldListTheCatalogueWithoutArchivedByDefault() {
        when(exercises.findAll(false)).thenReturn(List.of(anExercise()));

        var result = new ListExercisesQueryHandler(exercises).handle(new ListExercisesQuery(false));

        assertThat(result.value()).hasSize(1);
        assertThat(result.value().getFirst().name()).isEqualTo("Press banca");
        verify(exercises).findAll(false);
    }

    @Test
    void shouldPassTheArchivedFlagStraightThrough() {
        when(exercises.findAll(true)).thenReturn(List.of());

        new ListExercisesQueryHandler(exercises).handle(new ListExercisesQuery(true));

        verify(exercises).findAll(true);
    }

    @Test
    void shouldListTheTemplates() {
        when(workouts.findAll(false)).thenReturn(List.of(aWorkout()));

        var result = new ListWorkoutsQueryHandler(workouts).handle(new ListWorkoutsQuery(false));

        assertThat(result.value()).hasSize(1);
        assertThat(result.value().getFirst().id()).isEqualTo("W00000005");
    }

    @Test
    void shouldReturnATemplateWithItsPlan() {
        when(workouts.find(WORKOUT)).thenReturn(Optional.of(aWorkout()));

        var result = new GetWorkoutQueryHandler(workouts).handle(new GetWorkoutQuery(WORKOUT));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().name()).isEqualTo("Empuje");
    }

    @Test
    void shouldReportAnUnknownTemplateAsNotFound() {
        when(workouts.find(WORKOUT)).thenReturn(Optional.empty());

        var result = new GetWorkoutQueryHandler(workouts).handle(new GetWorkoutQuery(WORKOUT));

        assertThat(result.error()).isEqualTo(WorkoutErrors.notFound(WORKOUT));
    }

    @Test
    void shouldReturnALogWithItsSets() {
        when(logs.find(LOG)).thenReturn(Optional.of(aLog()));

        var result = new GetWorkoutLogQueryHandler(logs).handle(new GetWorkoutLogQuery(LOG));

        assertThat(result.value().id()).isEqualTo("T00000003");
    }

    @Test
    void shouldReportAnUnknownLogAsNotFound() {
        when(logs.find(LOG)).thenReturn(Optional.empty());

        var result = new GetWorkoutLogQueryHandler(logs).handle(new GetWorkoutLogQuery(LOG));

        assertThat(result.error()).isEqualTo(WorkoutLogErrors.notFound(LOG));
    }

    @Test
    void shouldDefaultTheHistoryToTheLastThirtyDaysEndingToday() {
        when(logs.findBetween(TODAY.minusDays(29), TODAY, 50)).thenReturn(List.of(aLog()));

        var result = new ListWorkoutLogsQueryHandler(logs, CLOCK)
            .handle(new ListWorkoutLogsQuery(null, null, null));

        assertThat(result.value()).hasSize(1);
        verify(logs).findBetween(TODAY.minusDays(29), TODAY, 50);
    }

    @Test
    void shouldCapTheLimitSoOneCallCannotDragTheWholeHistory() {
        var limit = ArgumentCaptor.forClass(Integer.class);
        when(logs.findBetween(any(), any(), anyInt())).thenReturn(List.of());

        new ListWorkoutLogsQueryHandler(logs, CLOCK)
            .handle(new ListWorkoutLogsQuery(TODAY, TODAY, 5_000));

        verify(logs).findBetween(any(), any(), limit.capture());
        assertThat(limit.getValue()).isEqualTo(ListWorkoutLogsQueryHandler.MAX_LIMIT);
    }

    @Test
    void shouldFallBackToTheDefaultLimitWhenAskedForNonsense() {
        var limit = ArgumentCaptor.forClass(Integer.class);
        when(logs.findBetween(any(), any(), anyInt())).thenReturn(List.of());

        new ListWorkoutLogsQueryHandler(logs, CLOCK)
            .handle(new ListWorkoutLogsQuery(TODAY, TODAY, 0));

        verify(logs).findBetween(any(), any(), limit.capture());
        assertThat(limit.getValue()).isEqualTo(ListWorkoutLogsQueryHandler.DEFAULT_LIMIT);
    }

    @Test
    void shouldReturnEveryLogOfTodayBecauseMorningAndEveningAreTwo() {
        when(logs.findOn(TODAY)).thenReturn(List.of(aLog(), aLog()));

        var result = new GetTodayWorkoutQueryHandler(logs, CLOCK).handle(new GetTodayWorkoutQuery());

        assertThat(result.value()).hasSize(2);
    }

    @Test
    void shouldReturnNothingWhenTodayHasNoWorkoutYet() {
        when(logs.findOn(TODAY)).thenReturn(List.of());

        var result = new GetTodayWorkoutQueryHandler(logs, CLOCK).handle(new GetTodayWorkoutQuery());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEmpty();
    }

    private static Exercise anExercise() {
        return Exercise.rehydrate(
            ExerciseId.of(10), new ExerciseName("Press banca"), Metric.LOAD, false);
    }

    private static Workout aWorkout() {
        return Workout.rehydrate(WORKOUT, new WorkoutName("Empuje"), List.of(), Set.of(), false);
    }

    private static WorkoutLog aLog() {
        return WorkoutLog.rehydrate(LOG, Optional.of(WORKOUT), TODAY, NOW, List.of());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
