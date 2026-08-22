package atlas.application.training.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.training.commands.archiveworkout.ArchiveWorkoutCommand;
import atlas.application.training.commands.archiveworkout.ArchiveWorkoutCommandHandler;
import atlas.application.training.commands.defineworkout.DefineWorkoutCommand;
import atlas.application.training.commands.defineworkout.DefineWorkoutCommandHandler;
import atlas.application.training.commands.renameworkout.RenameWorkoutCommand;
import atlas.application.training.commands.renameworkout.RenameWorkoutCommandHandler;
import atlas.application.training.commands.scheduleworkout.ScheduleWorkoutCommand;
import atlas.application.training.commands.scheduleworkout.ScheduleWorkoutCommandHandler;
import atlas.application.training.commands.setworkoutplan.SetWorkoutPlanCommand;
import atlas.application.training.commands.setworkoutplan.SetWorkoutPlanCommandHandler;
import atlas.application.training.ports.ExerciseRepository;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.application.training.ports.WorkoutRepository;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseErrors;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutErrors;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.enums.Metric;
import atlas.domain.training.vos.ExerciseName;
import atlas.domain.training.vos.WorkoutName;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkoutCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final WorkoutId ID = WorkoutId.of(5);
    private static final ExerciseId PRESS = ExerciseId.of(10);

    private final TrainingUnitOfWork unitOfWork = mock(TrainingUnitOfWork.class);
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final ExerciseRepository exercises = mock(ExerciseRepository.class);
    private final AtomicLong ids = new AtomicLong();

    private final DefineWorkoutCommandHandler define =
        new DefineWorkoutCommandHandler(unitOfWork, CLOCK);
    private final RenameWorkoutCommandHandler rename =
        new RenameWorkoutCommandHandler(unitOfWork, CLOCK);
    private final ScheduleWorkoutCommandHandler schedule =
        new ScheduleWorkoutCommandHandler(unitOfWork, CLOCK);
    private final ArchiveWorkoutCommandHandler archive =
        new ArchiveWorkoutCommandHandler(unitOfWork, CLOCK);
    private final SetWorkoutPlanCommandHandler setPlan = new SetWorkoutPlanCommandHandler(
        unitOfWork, CLOCK, () -> new UUID(0, ids.incrementAndGet()));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.withWorkouts(unitOfWork, workouts);
        when(unitOfWork.exercises()).thenReturn(exercises);
        when(workouts.nextId()).thenReturn(ID);
        when(exercises.get(PRESS)).thenReturn(Optional.of(anExercise(false)));
    }

    @Test
    void shouldPersistTheWorkoutWithAnEmptyPlan() {
        var result = define.handle(new DefineWorkoutCommand("Dia de empuje"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("W00000005");
        assertThat(result.value().name()).isEqualTo("Dia de empuje");
        assertThat(result.value().plan()).isEmpty();
        verify(workouts).create(any(Workout.class));
    }

    @Test
    void shouldRejectABlankNameBeforeTouchingTheRepository() {
        var result = define.handle(new DefineWorkoutCommand(" "));

        assertThat(result.error()).isEqualTo(WorkoutErrors.NAME_REQUIRED);
        verify(workouts, never()).create(any());
    }

    @Test
    void shouldRenameAnExistingWorkout() {
        var workout = aWorkout(false);
        when(workouts.get(ID)).thenReturn(Optional.of(workout));

        var result = rename.handle(new RenameWorkoutCommand(ID, "Empuje A"));

        assertThat(result.value().name()).isEqualTo("Empuje A");
        verify(workouts).update(workout);
    }

    @Test
    void shouldReportAnUnknownWorkoutAsNotFound() {
        when(workouts.get(ID)).thenReturn(Optional.empty());

        assertThat(rename.handle(new RenameWorkoutCommand(ID, "X")).error())
            .isEqualTo(WorkoutErrors.notFound(ID));
        assertThat(archive.handle(new ArchiveWorkoutCommand(ID)).error())
            .isEqualTo(WorkoutErrors.notFound(ID));
        assertThat(setPlan.handle(new SetWorkoutPlanCommand(ID, List.of())).error())
            .isEqualTo(WorkoutErrors.notFound(ID));
    }

    @Test
    void shouldStoreThePlanWithFreshIdsForEachLine() {
        var workout = aWorkout(false);
        when(workouts.get(ID)).thenReturn(Optional.of(workout));

        var result = setPlan.handle(new SetWorkoutPlanCommand(ID, List.of(aLine(4, "70"))));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().plan()).hasSize(1);
        assertThat(result.value().plan().getFirst().sets()).isEqualTo(4);
        assertThat(result.value().plan().getFirst().target().load()).isEqualByComparingTo("70");
        assertThat(result.value().plan().getFirst().id()).isEqualTo(new UUID(0, 1).toString());
        verify(workouts).update(workout);
    }

    @Test
    void shouldConvertTheLoadFromKilogramsToGrams() {
        when(workouts.get(ID)).thenReturn(Optional.of(aWorkout(false)));

        var result = setPlan.handle(new SetWorkoutPlanCommand(ID, List.of(aLine(4, "72.5"))));

        assertThat(result.value().plan().getFirst().target().load()).isEqualByComparingTo("72.5");
    }

    @Test
    void shouldRejectASetCountNobodyDoes() {
        when(workouts.get(ID)).thenReturn(Optional.of(aWorkout(false)));

        var result = setPlan.handle(new SetWorkoutPlanCommand(ID, List.of(aLine(48, "70"))));

        assertThat(result.error()).isEqualTo(WorkoutErrors.SET_COUNT_OUT_OF_RANGE);
        verify(workouts, never()).update(any());
    }

    @Test
    void shouldRejectAPlanThatNamesAnExerciseThatDoesNotExist() {
        when(workouts.get(ID)).thenReturn(Optional.of(aWorkout(false)));
        when(exercises.get(PRESS)).thenReturn(Optional.empty());

        var result = setPlan.handle(new SetWorkoutPlanCommand(ID, List.of(aLine(4, "70"))));

        assertThat(result.error()).isEqualTo(ExerciseErrors.notFound(PRESS));
        verify(workouts, never()).update(any());
    }

    @Test
    void shouldRejectAnArchivedExerciseInANewPlan() {
        when(workouts.get(ID)).thenReturn(Optional.of(aWorkout(false)));
        when(exercises.get(PRESS)).thenReturn(Optional.of(anExercise(true)));

        var result = setPlan.handle(new SetWorkoutPlanCommand(ID, List.of(aLine(4, "70"))));

        assertThat(result.error()).isEqualTo(WorkoutErrors.ARCHIVED_EXERCISE_NOT_ALLOWED);
    }

    @Test
    void shouldRefuseToChangeAnArchivedWorkout() {
        when(workouts.get(ID)).thenReturn(Optional.of(aWorkout(true)));

        assertThat(setPlan.handle(new SetWorkoutPlanCommand(ID, List.of(aLine(4, "70")))).error())
            .isEqualTo(WorkoutErrors.ALREADY_ARCHIVED);
    }

    @Test
    void shouldAssignAWorkoutToTheDaysOfTheWeekItIsTrainedOn() {
        var workout = aWorkout(false);
        when(workouts.get(ID)).thenReturn(Optional.of(workout));

        var result = schedule.handle(
            new ScheduleWorkoutCommand(ID, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)));

        assertThat(result.value().days()).containsExactly("MONDAY", "THURSDAY");
        verify(workouts).update(workout);
    }

    @Test
    void shouldNotScheduleAWorkoutThatIsNotThere() {
        when(workouts.get(ID)).thenReturn(Optional.empty());

        var result = schedule.handle(
            new ScheduleWorkoutCommand(ID, Set.of(DayOfWeek.MONDAY)));

        assertThat(result.error()).isEqualTo(WorkoutErrors.notFound(ID));
        verify(workouts, never()).update(any());
    }

    @Test
    void shouldArchiveAnExistingWorkout() {
        var workout = aWorkout(false);
        when(workouts.get(ID)).thenReturn(Optional.of(workout));

        assertThat(archive.handle(new ArchiveWorkoutCommand(ID)).isSuccess()).isTrue();
        assertThat(workout.isArchived()).isTrue();
        verify(workouts).update(workout);
    }

    private static Workout aWorkout(boolean archived) {
        return Workout.rehydrate(ID, new WorkoutName("Dia de empuje"), List.of(), Set.of(), archived);
    }

    private static Exercise anExercise(boolean archived) {
        return Exercise.rehydrate(PRESS, new ExerciseName("Press banca"), Metric.LOAD, archived);
    }

    private static PlannedLineInput aLine(int sets, String load) {
        return new PlannedLineInput(PRESS, sets, new EffortInput(new BigDecimal(load), 8, 0, 0));
    }
}
