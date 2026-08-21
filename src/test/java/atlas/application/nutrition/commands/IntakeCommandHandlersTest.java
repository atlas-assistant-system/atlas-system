package atlas.application.nutrition.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.nutrition.commands.correctintake.CorrectIntakeCommand;
import atlas.application.nutrition.commands.correctintake.CorrectIntakeCommandHandler;
import atlas.application.nutrition.commands.deleteintake.DeleteIntakeCommand;
import atlas.application.nutrition.commands.deleteintake.DeleteIntakeCommandHandler;
import atlas.application.nutrition.commands.recordintake.RecordIntakeCommand;
import atlas.application.nutrition.commands.recordintake.RecordIntakeCommandHandler;
import atlas.application.nutrition.ports.IntakeRepository;
import atlas.application.nutrition.ports.NutritionUnitOfWork;
import atlas.domain.nutrition.Intake;
import atlas.domain.nutrition.IntakeErrors;
import atlas.domain.nutrition.IntakeId;
import atlas.domain.nutrition.NutritionErrors;
import atlas.domain.nutrition.events.IntakeDeletedEvent;
import atlas.domain.nutrition.events.IntakeRecordedEvent;
import atlas.domain.nutrition.vos.IntakeNote;
import atlas.domain.nutrition.vos.Macros;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IntakeCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-22T13:45:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final IntakeId ID = IntakeId.of(7);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final NutritionUnitOfWork unitOfWork = mock(NutritionUnitOfWork.class);
    private final IntakeRepository intakes = mock(IntakeRepository.class);

    private final RecordIntakeCommandHandler record = new RecordIntakeCommandHandler(unitOfWork, CLOCK);
    private final CorrectIntakeCommandHandler correct = new CorrectIntakeCommandHandler(unitOfWork, CLOCK);
    private final DeleteIntakeCommandHandler delete = new DeleteIntakeCommandHandler(unitOfWork, CLOCK);

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.withIntakes(unitOfWork, intakes);
        when(intakes.nextId()).thenReturn(ID);
    }

    @Test
    void shouldPersistTheIntakeWithItsDerivedCalories() {
        var result = record.handle(anIntakeOf(30, 60, 10, "Tortilla y pan", TODAY));

        assertThat(result.isSuccess()).isTrue();

        var intake = result.value();
        assertThat(intake.id()).isEqualTo("I00000007");
        assertThat(intake.macros().protein()).isEqualTo(30);
        assertThat(intake.macros().calories()).isEqualTo(450);
        assertThat(intake.note()).isEqualTo("Tortilla y pan");
        assertThat(intake.consumedOn()).isEqualTo(TODAY);

        verify(intakes).create(any(Intake.class));
    }

    @Test
    void shouldRegisterTheRecordedEvent() {
        record.handle(anIntakeOf(30, 60, 10, null, TODAY));

        var saved = ArgumentCaptor.forClass(Intake.class);
        verify(intakes).create(saved.capture());

        assertThat(saved.getValue().pendingEvents()).containsExactly(new IntakeRecordedEvent(ID, NOW));
    }

    @Test
    void shouldDateTheIntakeTodayWhenNoDateIsGiven() {
        assertThat(record.handle(anIntakeOf(30, 60, 10, null, null)).value().consumedOn())
            .isEqualTo(TODAY);
    }

    @Test
    void shouldFailWhenNoMacroWasEaten() {
        var result = record.handle(anIntakeOf(0, 0, 0, null, TODAY));

        assertThat(result.error()).isEqualTo(IntakeErrors.MACROS_REQUIRED);
        verify(intakes, never()).create(any());
    }

    @Test
    void shouldFailWhenAMacroIsNegative() {
        var result = record.handle(anIntakeOf(30, -1, 10, null, TODAY));

        assertThat(result.error()).isEqualTo(NutritionErrors.MACROS_MUST_NOT_BE_NEGATIVE);
        verify(intakes, never()).create(any());
    }

    @Test
    void shouldFailWhenTheNoteIsTooLong() {
        var result = record.handle(anIntakeOf(30, 60, 10, "x".repeat(200), TODAY));

        assertThat(result.error()).isEqualTo(IntakeErrors.NOTE_TOO_LONG);
        verify(intakes, never()).create(any());
    }

    @Test
    void shouldFailWhenTheIntakeIsDatedInTheFuture() {
        var result = record.handle(anIntakeOf(30, 60, 10, null, TODAY.plusDays(1)));

        assertThat(result.error()).isEqualTo(IntakeErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        verify(intakes, never()).create(any());
    }

    @Test
    void shouldCorrectTheIntake() {
        var intake = anExistingIntake();
        when(intakes.get(ID)).thenReturn(Optional.of(intake));

        var result = correct.handle(new CorrectIntakeCommand(ID, 35, 55, 12, null));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().macros().protein()).isEqualTo(35);
        assertThat(result.value().note()).isNull();

        verify(intakes).update(intake);
    }

    @Test
    void shouldKeepTheDayWhenTheIntakeIsCorrected() {
        when(intakes.get(ID)).thenReturn(Optional.of(anExistingIntake()));

        var result = correct.handle(new CorrectIntakeCommand(ID, 35, 55, 12, null));

        assertThat(result.value().consumedOn()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void shouldFailToCorrectAnIntakeThatIsNotThere() {
        when(intakes.get(ID)).thenReturn(Optional.empty());

        var result = correct.handle(new CorrectIntakeCommand(ID, 35, 55, 12, null));

        assertThat(result.error()).isEqualTo(IntakeErrors.notFound(ID));
        verify(intakes, never()).update(any());
    }

    @Test
    void shouldFailWhenTheCorrectionLeavesNoMacros() {
        when(intakes.get(ID)).thenReturn(Optional.of(anExistingIntake()));

        var result = correct.handle(new CorrectIntakeCommand(ID, 0, 0, 0, null));

        assertThat(result.error()).isEqualTo(IntakeErrors.MACROS_REQUIRED);
        verify(intakes, never()).update(any());
    }

    @Test
    void shouldDeleteTheIntakeAndRegisterItsEvent() {
        var intake = anExistingIntake();
        when(intakes.get(ID)).thenReturn(Optional.of(intake));

        var result = delete.handle(new DeleteIntakeCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(intake.pendingEvents()).contains(new IntakeDeletedEvent(ID, NOW));

        verify(intakes).delete(intake);
    }

    @Test
    void shouldFailToDeleteAnIntakeThatIsNotThere() {
        when(intakes.get(ID)).thenReturn(Optional.empty());

        var result = delete.handle(new DeleteIntakeCommand(ID));

        assertThat(result.error()).isEqualTo(IntakeErrors.notFound(ID));
        verify(intakes, never()).delete(any());
    }

    private static Intake anExistingIntake() {
        return Intake.rehydrate(
            ID, new Macros(30, 60, 10), Optional.of(new IntakeNote("Tortilla")), TODAY.minusDays(1), NOW);
    }

    private static RecordIntakeCommand anIntakeOf(
        int protein, int carbs, int fat, String note, LocalDate consumedOn) {

        return new RecordIntakeCommand(protein, carbs, fat, note, consumedOn);
    }
}
