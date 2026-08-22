package atlas.infrastructure.training.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.application.training.ports.ExerciseRepository;
import atlas.application.training.ports.WorkoutLogRepository;
import atlas.application.training.ports.WorkoutRepository;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.entities.PlannedExerciseId;
import atlas.domain.training.entities.SetLogId;
import atlas.domain.training.enums.Metric;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.ExerciseName;
import atlas.domain.training.vos.PlannedLine;
import atlas.domain.training.vos.SetCount;
import atlas.domain.training.vos.WorkoutName;
import atlas.infrastructure.common.SqliteSequenceGenerator;
import atlas.support.builders.TrainingTestDatabase;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteTrainingPersistenceIT {

    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Effort EIGHT_AT_SEVENTY = new Effort(70_000, 8, 0, 0);

    @TempDir
    private Path directory;

    private Connection connection;
    private SqliteTrainingUnitOfWork unitOfWork;
    private ExerciseRepository exercises;
    private WorkoutRepository workouts;
    private WorkoutLogRepository logs;
    private SqliteWorkoutLogReadModel logsRead;
    private SqliteExerciseReadModel exercisesRead;
    private final AtomicLong uuids = new AtomicLong();

    @BeforeEach
    void openDatabase() {
        connection = TrainingTestDatabase.open(directory, CLOCK);
        var sequences = new SqliteSequenceGenerator(connection);
        unitOfWork = new SqliteTrainingUnitOfWork(
            connection,
            new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())),
            sequences);
        exercises = unitOfWork.exercises();
        workouts = unitOfWork.workouts();
        logs = unitOfWork.logs();
        logsRead = new SqliteWorkoutLogReadModel(connection);
        exercisesRead = new SqliteExerciseReadModel(connection);
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        connection.close();
    }

    @Test
    void shouldReadBackTheExerciseItStored() {
        var exercise = anExercise("Press banca", Metric.LOAD);
        unitOfWork.run(() -> exercises.create(exercise));

        var found = exercises.get(exercise.id()).orElseThrow();

        assertThat(found.name().value()).isEqualTo("Press banca");
        assertThat(found.metric()).isEqualTo(Metric.LOAD);
        assertThat(found.isArchived()).isFalse();
    }

    @Test
    void shouldFindAnExerciseByItsName() {
        unitOfWork.run(() -> exercises.create(anExercise("Dominadas", Metric.REPS)));

        assertThat(exercises.findByName(new ExerciseName("Dominadas"))).isPresent();
        assertThat(exercises.findByName(new ExerciseName("Fondos"))).isEmpty();
    }

    @Test
    void shouldLeaveArchivedExercisesOutUnlessAskedForThem() {
        var archived = anExercise("Peso muerto", Metric.LOAD);
        archived.archive(NOW);
        unitOfWork.run(() -> exercises.create(archived));
        unitOfWork.run(() -> exercises.create(anExercise("Press banca", Metric.LOAD)));

        assertThat(exercisesRead.findAll(false)).hasSize(1);
        assertThat(exercisesRead.findAll(true)).hasSize(2);
    }

    @Test
    void shouldReadBackTheWholePlanInItsOrder() {
        var press = anExercise("Press banca", Metric.LOAD);
        var dips = anExercise("Fondos", Metric.REPS);
        unitOfWork.run(() -> exercises.create(press));
        unitOfWork.run(() -> exercises.create(dips));

        var workout = aWorkout();
        workout.setPlan(List.of(line(press.id(), 4), line(dips.id(), 3)), NOW);
        unitOfWork.run(() -> workouts.create(workout));

        var found = workouts.get(workout.id()).orElseThrow();

        assertThat(found.name().value()).isEqualTo("Dia de empuje");
        assertThat(found.plan()).extracting(l -> l.exerciseId()).containsExactly(press.id(), dips.id());
        assertThat(found.plan().getFirst().sets().value()).isEqualTo(4);
        assertThat(found.plan().getFirst().target()).isEqualTo(EIGHT_AT_SEVENTY);
    }

    @Test
    void shouldKeepTheDaysOfTheWeekAWorkoutIsTrainedOn() {
        var workout = aWorkout();
        workout.scheduleOn(Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), NOW);
        unitOfWork.run(() -> workouts.create(workout));

        var found = workouts.get(workout.id()).orElseThrow();

        assertThat(found.days()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
    }

    @Test
    void shouldLeaveAWorkoutOffTheWeekWhenItHasNoDayAssigned() {
        var workout = aWorkout();
        unitOfWork.run(() -> workouts.create(workout));

        assertThat(workouts.get(workout.id()).orElseThrow().days()).isEmpty();
    }

    @Test
    void shouldLeaveNoOrphanLinesWhenThePlanIsReplaced() {
        var press = anExercise("Press banca", Metric.LOAD);
        unitOfWork.run(() -> exercises.create(press));

        var workout = aWorkout();
        workout.setPlan(List.of(line(press.id(), 4), line(press.id(), 2)), NOW);
        unitOfWork.run(() -> workouts.create(workout));

        workout.setPlan(List.of(line(press.id(), 5)), NOW);
        unitOfWork.run(() -> workouts.update(workout));

        assertThat(workouts.get(workout.id()).orElseThrow().plan()).hasSize(1);
        assertThat(countOf("workout_exercises")).isEqualTo(1);
    }

    @Test
    void shouldLeaveNoOrphanLinesWhenTheWorkoutIsDeleted() {
        var press = anExercise("Press banca", Metric.LOAD);
        unitOfWork.run(() -> exercises.create(press));

        var workout = aWorkout();
        workout.setPlan(List.of(line(press.id(), 4)), NOW);
        unitOfWork.run(() -> workouts.create(workout));

        unitOfWork.run(() -> workouts.delete(workout));

        assertThat(countOf("workout_exercises")).isZero();
    }

    @Test
    void shouldKeepThePlannedEffortFrozenAndTheActualOneApart() {
        var press = anExercise("Press banca", Metric.LOAD);
        unitOfWork.run(() -> exercises.create(press));
        var workout = aWorkout();
        workout.setPlan(List.of(line(press.id(), 2)), NOW);
        unitOfWork.run(() -> workouts.create(workout));

        var log = startFrom(workout);
        log.recordSet(log.sets().getFirst().id(), new Effort(70_000, 6, 0, 0), NOW);
        unitOfWork.run(() -> logs.create(log));

        var found = logs.get(log.id()).orElseThrow();

        assertThat(found.sets()).hasSize(2);
        assertThat(found.sets().getFirst().planned()).contains(EIGHT_AT_SEVENTY);
        assertThat(found.sets().getFirst().actual()).contains(new Effort(70_000, 6, 0, 0));
        assertThat(found.sets().getLast().actual()).isEmpty();
    }

    @Test
    void shouldNotRewriteHistoryWhenTheTemplateChangesAfterwards() {
        var press = anExercise("Press banca", Metric.LOAD);
        unitOfWork.run(() -> exercises.create(press));
        var workout = aWorkout();
        workout.setPlan(List.of(line(press.id(), 1)), NOW);
        unitOfWork.run(() -> workouts.create(workout));

        var log = startFrom(workout);
        unitOfWork.run(() -> logs.create(log));

        workout.setPlan(List.of(new PlannedLine(
            PlannedExerciseId.of(nextUuid()), press.id(), new SetCount(5), new Effort(90_000, 5, 0, 0))), NOW);
        unitOfWork.run(() -> workouts.update(workout));

        assertThat(logs.get(log.id()).orElseThrow().sets().getFirst().planned())
            .contains(EIGHT_AT_SEVENTY);
    }

    @Test
    void shouldKeepAFreeWorkoutWithoutATemplate() {
        var log = WorkoutLog.start(
            logs.nextId(), Optional.empty(), List.of(), this::nextSetId, TODAY, TODAY, NOW).value();
        unitOfWork.run(() -> logs.create(log));

        assertThat(logs.get(log.id()).orElseThrow().workoutId()).isEmpty();
    }

    @Test
    void shouldFindOnlyTheLogsOfTheDayAsked() {
        var today = aFreeLogOn(TODAY);
        var yesterday = aFreeLogOn(TODAY.minusDays(1));
        unitOfWork.run(() -> logs.create(today));
        unitOfWork.run(() -> logs.create(yesterday));

        assertThat(logsRead.findOn(TODAY)).extracting(WorkoutLog::id).containsExactly(today.id());
    }

    @Test
    void shouldListTheRangeNewestFirstAndHonourTheLimit() {
        var first = aFreeLogOn(TODAY.minusDays(2));
        var second = aFreeLogOn(TODAY.minusDays(1));
        var third = aFreeLogOn(TODAY);
        unitOfWork.run(() -> logs.create(first));
        unitOfWork.run(() -> logs.create(second));
        unitOfWork.run(() -> logs.create(third));

        assertThat(logsRead.findBetween(TODAY.minusDays(2), TODAY, 10))
            .extracting(WorkoutLog::id)
            .containsExactly(third.id(), second.id(), first.id());
        assertThat(logsRead.findBetween(TODAY.minusDays(2), TODAY, 2)).hasSize(2);
        assertThat(logsRead.findBetween(TODAY.minusDays(1), TODAY, 10)).hasSize(2);
    }

    @Test
    void shouldAttachEachSetToItsOwnLogWhenReadingARange() {
        var press = anExercise("Press banca", Metric.LOAD);
        unitOfWork.run(() -> exercises.create(press));
        var workout = aWorkout();
        workout.setPlan(List.of(line(press.id(), 2)), NOW);
        unitOfWork.run(() -> workouts.create(workout));

        var older = startFrom(workout, TODAY.minusDays(1));
        var newer = startFrom(workout, TODAY);
        unitOfWork.run(() -> logs.create(older));
        unitOfWork.run(() -> logs.create(newer));

        var found = logsRead.findBetween(TODAY.minusDays(1), TODAY, 10);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).sets()).hasSize(2);
        assertThat(found.get(1).sets()).hasSize(2);
        assertThat(found.get(0).sets().getFirst().id())
            .isNotEqualTo(found.get(1).sets().getFirst().id());
    }

    @Test
    void shouldLeaveNoOrphanSetsWhenTheLogIsDiscarded() {
        var press = anExercise("Press banca", Metric.LOAD);
        unitOfWork.run(() -> exercises.create(press));
        var workout = aWorkout();
        workout.setPlan(List.of(line(press.id(), 3)), NOW);
        unitOfWork.run(() -> workouts.create(workout));

        var log = startFrom(workout);
        unitOfWork.run(() -> logs.create(log));
        unitOfWork.run(() -> logs.delete(log));

        assertThat(countOf("set_logs")).isZero();
    }

    @Test
    void shouldHandOutADifferentIdPerAggregate() {
        assertThat(exercises.nextId().value()).isEqualTo(1);
        assertThat(exercises.nextId().value()).isEqualTo(2);
        assertThat(workouts.nextId().value()).isEqualTo(1);
        assertThat(logs.nextId().value()).isEqualTo(1);
    }

    private Exercise anExercise(String name, Metric metric) {
        return Exercise.define(exercises.nextId(), new ExerciseName(name), metric, NOW).value();
    }

    private Workout aWorkout() {
        return Workout.define(workouts.nextId(), new WorkoutName("Dia de empuje"), NOW).value();
    }

    private PlannedLine line(ExerciseId exerciseId, int sets) {
        return new PlannedLine(
            PlannedExerciseId.of(nextUuid()), exerciseId, new SetCount(sets), EIGHT_AT_SEVENTY);
    }

    private WorkoutLog startFrom(Workout workout) {
        return startFrom(workout, TODAY);
    }

    private WorkoutLog startFrom(Workout workout, LocalDate day) {
        return WorkoutLog.start(
            logs.nextId(), Optional.of(workout.id()), workout.expand(),
            this::nextSetId, day, TODAY, NOW).value();
    }

    private WorkoutLog aFreeLogOn(LocalDate day) {
        return WorkoutLog.start(
            logs.nextId(), Optional.empty(), List.of(), this::nextSetId, day, TODAY, NOW).value();
    }

    private SetLogId nextSetId() {
        return SetLogId.of(nextUuid());
    }

    private UUID nextUuid() {
        return new UUID(0, uuids.incrementAndGet());
    }

    private int countOf(String table) {
        try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
            var rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
