package atlas.application.training.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.training.commands.archiveexercise.ArchiveExerciseCommand;
import atlas.application.training.commands.archiveexercise.ArchiveExerciseCommandHandler;
import atlas.application.training.commands.defineexercise.DefineExerciseCommand;
import atlas.application.training.commands.defineexercise.DefineExerciseCommandHandler;
import atlas.application.training.commands.renameexercise.RenameExerciseCommand;
import atlas.application.training.commands.renameexercise.RenameExerciseCommandHandler;
import atlas.application.training.commands.unarchiveexercise.UnarchiveExerciseCommand;
import atlas.application.training.commands.unarchiveexercise.UnarchiveExerciseCommandHandler;
import atlas.application.training.ports.ExerciseRepository;
import atlas.application.training.ports.TrainingUnitOfWork;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseErrors;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.enums.Metric;
import atlas.domain.training.events.ExerciseDefinedEvent;
import atlas.domain.training.vos.ExerciseName;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExerciseCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T18:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ExerciseId ID = ExerciseId.of(7);

    private final TrainingUnitOfWork unitOfWork = mock(TrainingUnitOfWork.class);
    private final ExerciseRepository exercises = mock(ExerciseRepository.class);

    private final DefineExerciseCommandHandler define =
        new DefineExerciseCommandHandler(unitOfWork, CLOCK);
    private final RenameExerciseCommandHandler rename =
        new RenameExerciseCommandHandler(unitOfWork, CLOCK);
    private final ArchiveExerciseCommandHandler archive =
        new ArchiveExerciseCommandHandler(unitOfWork, CLOCK);
    private final UnarchiveExerciseCommandHandler unarchive =
        new UnarchiveExerciseCommandHandler(unitOfWork, CLOCK);

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.withExercises(unitOfWork, exercises);
        when(exercises.nextId()).thenReturn(ID);
        when(exercises.findByName(any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldPersistTheExerciseAndReturnItWithItsMetric() {
        var result = define.handle(new DefineExerciseCommand("Press banca", Metric.LOAD));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("E00000007");
        assertThat(result.value().name()).isEqualTo("Press banca");
        assertThat(result.value().metric()).isEqualTo("LOAD");
        verify(exercises).create(any(Exercise.class));
    }

    @Test
    void shouldRegisterTheDefinedEvent() {
        define.handle(new DefineExerciseCommand("Press banca", Metric.LOAD));

        var captor = ArgumentCaptor.forClass(Exercise.class);
        verify(exercises).create(captor.capture());
        assertThat(captor.getValue().pendingEvents())
            .containsExactly(new ExerciseDefinedEvent(ID, NOW));
    }

    @Test
    void shouldRejectABlankNameBeforeTouchingTheRepository() {
        var result = define.handle(new DefineExerciseCommand("  ", Metric.LOAD));

        assertThat(result.error()).isEqualTo(ExerciseErrors.NAME_REQUIRED);
        verify(exercises, never()).create(any());
    }

    @Test
    void shouldRefuseASecondExerciseWithTheSameName() {
        when(exercises.findByName(new ExerciseName("Press banca")))
            .thenReturn(Optional.of(anExercise(false)));

        var result = define.handle(new DefineExerciseCommand("Press banca", Metric.LOAD));

        assertThat(result.error()).isEqualTo(ExerciseErrors.NAME_ALREADY_TAKEN);
        verify(exercises, never()).create(any());
    }

    @Test
    void shouldRenameAnExistingExercise() {
        var exercise = anExercise(false);
        when(exercises.get(ID)).thenReturn(Optional.of(exercise));

        var result = rename.handle(new RenameExerciseCommand(ID, "Press inclinado"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().name()).isEqualTo("Press inclinado");
        verify(exercises).update(exercise);
    }

    @Test
    void shouldNotRenameToANameAnotherExerciseAlreadyHas() {
        when(exercises.get(ID)).thenReturn(Optional.of(anExercise(false)));
        when(exercises.findByName(new ExerciseName("Fondos")))
            .thenReturn(Optional.of(Exercise.rehydrate(
                ExerciseId.of(99), new ExerciseName("Fondos"), Metric.REPS, false)));

        var result = rename.handle(new RenameExerciseCommand(ID, "Fondos"));

        assertThat(result.error()).isEqualTo(ExerciseErrors.NAME_ALREADY_TAKEN);
        verify(exercises, never()).update(any());
    }

    @Test
    void shouldLetAnExerciseKeepItsOwnNameWhenRenamingToTheSameThing() {
        var exercise = anExercise(false);
        when(exercises.get(ID)).thenReturn(Optional.of(exercise));
        when(exercises.findByName(new ExerciseName("Press banca"))).thenReturn(Optional.of(exercise));

        var result = rename.handle(new RenameExerciseCommand(ID, "Press banca"));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldReportAnUnknownExerciseAsNotFound() {
        when(exercises.get(ID)).thenReturn(Optional.empty());

        assertThat(rename.handle(new RenameExerciseCommand(ID, "X")).error())
            .isEqualTo(ExerciseErrors.notFound(ID));
        assertThat(archive.handle(new ArchiveExerciseCommand(ID)).error())
            .isEqualTo(ExerciseErrors.notFound(ID));
        assertThat(unarchive.handle(new UnarchiveExerciseCommand(ID)).error())
            .isEqualTo(ExerciseErrors.notFound(ID));
    }

    @Test
    void shouldArchiveAndUnarchive() {
        var exercise = anExercise(false);
        when(exercises.get(ID)).thenReturn(Optional.of(exercise));

        assertThat(archive.handle(new ArchiveExerciseCommand(ID)).isSuccess()).isTrue();
        assertThat(exercise.isArchived()).isTrue();

        assertThat(unarchive.handle(new UnarchiveExerciseCommand(ID)).isSuccess()).isTrue();
        assertThat(exercise.isArchived()).isFalse();
    }

    @Test
    void shouldNotArchiveWhatIsAlreadyArchived() {
        when(exercises.get(ID)).thenReturn(Optional.of(anExercise(true)));

        var result = archive.handle(new ArchiveExerciseCommand(ID));

        assertThat(result.error()).isEqualTo(ExerciseErrors.ALREADY_ARCHIVED);
        verify(exercises, never()).update(any());
    }

    private static Exercise anExercise(boolean archived) {
        return Exercise.rehydrate(ID, new ExerciseName("Press banca"), Metric.LOAD, archived);
    }
}
