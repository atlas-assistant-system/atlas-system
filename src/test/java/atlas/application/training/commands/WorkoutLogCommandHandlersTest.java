package atlas.application.training.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.training.commands.addset.AddSetCommand;
import atlas.application.training.commands.addset.AddSetCommandHandler;
import atlas.application.training.commands.discardworkoutlog.DiscardWorkoutLogCommand;
import atlas.application.training.commands.discardworkoutlog.DiscardWorkoutLogCommandHandler;
import atlas.application.training.commands.recordset.RecordSetCommand;
import atlas.application.training.commands.recordset.RecordSetCommandHandler;
import atlas.application.training.commands.removeset.RemoveSetCommand;
import atlas.application.training.commands.removeset.RemoveSetCommandHandler;
import atlas.application.training.commands.startworkoutlog.StartWorkoutLogCommand;
import atlas.application.training.commands.startworkoutlog.StartWorkoutLogCommandHandler;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.application.training.ports.WorkoutLogRepository;
import atlas.application.training.ports.WorkoutRepository;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutErrors;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.WorkoutLogErrors;
import atlas.domain.training.WorkoutLogId;
import atlas.domain.training.entities.PlannedExercise;
import atlas.domain.training.entities.PlannedExerciseId;
import atlas.domain.training.entities.SetLogId;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.SetCount;
import atlas.domain.training.vos.WorkoutName;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkoutLogCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final WorkoutLogId LOG = WorkoutLogId.of(3);
    private static final WorkoutId WORKOUT = WorkoutId.of(5);
    private static final ExerciseId PRESS = ExerciseId.of(10);

    private final TrainingUnitOfWork unitOfWork = mock(TrainingUnitOfWork.class);
    private final WorkoutLogRepository logs = mock(WorkoutLogRepository.class);
    private final WorkoutRepository workouts = mock(WorkoutRepository.class);
    private final AtomicLong ids = new AtomicLong();

    private final StartWorkoutLogCommandHandler start = new StartWorkoutLogCommandHandler(
        unitOfWork, CLOCK, () -> new UUID(0, ids.incrementAndGet()));
    private final RecordSetCommandHandler record = new RecordSetCommandHandler(unitOfWork, CLOCK);
    private final AddSetCommandHandler add =
        new AddSetCommandHandler(unitOfWork, CLOCK, () -> new UUID(0, ids.incrementAndGet()));
    private final RemoveSetCommandHandler remove = new RemoveSetCommandHandler(unitOfWork, CLOCK);
    private final DiscardWorkoutLogCommandHandler discard =
        new DiscardWorkoutLogCommandHandler(unitOfWork, CLOCK);

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.withLogs(unitOfWork, logs, workouts);
        when(logs.nextId()).thenReturn(LOG);
        when(workouts.get(WORKOUT)).thenReturn(Optional.of(aWorkoutOf(4)));
    }

    @Test
    void shouldExpandTheTemplateIntoPendingSetsWhenStartingFromIt() {
        var result = start.handle(new StartWorkoutLogCommand(WORKOUT, null));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("T00000003");
        assertThat(result.value().workoutId()).isEqualTo("W00000005");
        assertThat(result.value().performedOn()).isEqualTo(TODAY);
        assertThat(result.value().sets()).hasSize(4);
        assertThat(result.value().sets().getFirst().planned().reps()).isEqualTo(8);
        assertThat(result.value().sets().getFirst().actual()).isNull();
        verify(logs).create(any(WorkoutLog.class));
    }

    @Test
    void shouldStartAFreeWorkoutWhenNoTemplateIsNamed() {
        var result = start.handle(new StartWorkoutLogCommand(null, null));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().workoutId()).isNull();
        assertThat(result.value().sets()).isEmpty();
    }

    @Test
    void shouldReportAnUnknownTemplateAsNotFound() {
        when(workouts.get(WORKOUT)).thenReturn(Optional.empty());

        var result = start.handle(new StartWorkoutLogCommand(WORKOUT, null));

        assertThat(result.error()).isEqualTo(WorkoutErrors.notFound(WORKOUT));
        verify(logs, never()).create(any());
    }

    @Test
    void shouldNotStartAWorkoutDatedInTheFuture() {
        var result = start.handle(new StartWorkoutLogCommand(null, TODAY.plusDays(1)));

        assertThat(result.error()).isEqualTo(WorkoutLogErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
    }

    @Test
    void shouldRecordWhatWasActuallyLifted() {
        var log = aStartedLog();
        var setId = log.sets().getFirst().id();
        when(logs.get(LOG)).thenReturn(Optional.of(log));

        var result = record.handle(
            new RecordSetCommand(LOG, setId, new EffortInput(new BigDecimal("72.5"), 6, 0, 0)));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().sets().getFirst().actual().load()).isEqualByComparingTo("72.5");
        assertThat(result.value().sets().getFirst().actual().reps()).isEqualTo(6);
        assertThat(result.value().sets().getFirst().planned().reps()).isEqualTo(8);
        verify(logs).update(log);
    }

    @Test
    void shouldRefuseASetThatMeasuresNothing() {
        var log = aStartedLog();
        when(logs.get(LOG)).thenReturn(Optional.of(log));

        var result = record.handle(new RecordSetCommand(
            LOG, log.sets().getFirst().id(), new EffortInput(BigDecimal.ZERO, 0, 0, 0)));

        assertThat(result.error()).isEqualTo(WorkoutLogErrors.SET_MEASURES_NOTHING);
        verify(logs, never()).update(any());
    }

    @Test
    void shouldReportAnUnknownLogAsNotFound() {
        when(logs.get(LOG)).thenReturn(Optional.empty());
        var stranger = SetLogId.of(new UUID(0, 99));

        assertThat(record.handle(new RecordSetCommand(
            LOG, stranger, new EffortInput(BigDecimal.ONE, 1, 0, 0))).error())
            .isEqualTo(WorkoutLogErrors.notFound(LOG));
        assertThat(remove.handle(new RemoveSetCommand(LOG, stranger)).error())
            .isEqualTo(WorkoutLogErrors.notFound(LOG));
        assertThat(discard.handle(new DiscardWorkoutLogCommand(LOG)).error())
            .isEqualTo(WorkoutLogErrors.notFound(LOG));
        assertThat(add.handle(new AddSetCommand(
            LOG, PRESS, new EffortInput(BigDecimal.ONE, 1, 0, 0))).error())
            .isEqualTo(WorkoutLogErrors.notFound(LOG));
    }

    @Test
    void shouldAddASetOutsideTheScript() {
        var log = aStartedLog();
        when(logs.get(LOG)).thenReturn(Optional.of(log));

        var result = add.handle(
            new AddSetCommand(LOG, PRESS, new EffortInput(new BigDecimal("60"), 12, 0, 0)));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().sets()).hasSize(5);
        assertThat(result.value().sets().getLast().planned()).isNull();
        assertThat(result.value().sets().getLast().actual().reps()).isEqualTo(12);
        verify(logs).update(log);
    }

    @Test
    void shouldRemoveASet() {
        var log = aStartedLog();
        when(logs.get(LOG)).thenReturn(Optional.of(log));

        var result = remove.handle(new RemoveSetCommand(LOG, log.sets().getFirst().id()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(log.sets()).hasSize(3);
        verify(logs).update(log);
    }

    @Test
    void shouldDeleteTheWholeLogWhenDiscarded() {
        var log = aStartedLog();
        when(logs.get(LOG)).thenReturn(Optional.of(log));

        var result = discard.handle(new DiscardWorkoutLogCommand(LOG));

        assertThat(result.isSuccess()).isTrue();
        verify(logs).delete(log);
    }

    private static Workout aWorkoutOf(int sets) {
        var line = new PlannedExercise(
            PlannedExerciseId.of(new UUID(1, 1)), PRESS, 0,
            new SetCount(sets), new Effort(70_000, 8, 0, 0));

        return Workout.rehydrate(WORKOUT, new WorkoutName("Empuje"), List.of(line), false);
    }

    private WorkoutLog aStartedLog() {
        return WorkoutLog.start(
            LOG, Optional.of(WORKOUT), aWorkoutOf(4).expand(),
            () -> SetLogId.of(new UUID(0, ids.incrementAndGet())), TODAY, TODAY, NOW).value();
    }
}
