package atlas.domain.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.nutrition.events.IntakeCorrectedEvent;
import atlas.domain.nutrition.events.IntakeDeletedEvent;
import atlas.domain.nutrition.events.IntakeRecordedEvent;
import atlas.domain.nutrition.vos.Calories;
import atlas.domain.nutrition.vos.IntakeNote;
import atlas.domain.nutrition.vos.Macros;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IntakeTest {

    private static final IntakeId ID = IntakeId.of(1);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);
    private static final Instant NOW = Instant.parse("2026-08-22T13:45:00Z");
    private static final Macros MACROS = new Macros(30, 60, 10);
    private static final Optional<IntakeNote> NOTE = Optional.of(new IntakeNote("Tortilla y pan"));

    @Test
    void shouldRecordTheIntakeAndRaiseItsEvent() {
        var result = record(MACROS, TODAY);

        assertThat(result.isSuccess()).isTrue();

        var intake = result.value();
        assertThat(intake.macros()).isEqualTo(MACROS);
        assertThat(intake.note()).isEqualTo(NOTE);
        assertThat(intake.consumedOn()).isEqualTo(TODAY);
        assertThat(intake.recordedAt()).isEqualTo(NOW);
        assertThat(intake.pendingEvents()).containsExactly(new IntakeRecordedEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenNoMacroWasEaten() {
        var result = record(new Macros(0, 0, 0), TODAY);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(IntakeErrors.MACROS_REQUIRED);
    }

    @Test
    void shouldFailWhenItIsDatedInTheFuture() {
        var result = record(MACROS, TODAY.plusDays(1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(IntakeErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
    }

    @Test
    void shouldAcceptAnIntakeFromAnEarlierDay() {
        assertThat(record(MACROS, TODAY.minusDays(3)).isSuccess()).isTrue();
    }

    @Test
    void shouldDeriveTheCaloriesFromTheMacros() {
        assertThat(record(MACROS, TODAY).value().calories()).isEqualTo(new Calories(450));
    }

    @Test
    void shouldCorrectTheMacrosAndTheNoteAndRaiseItsEvent() {
        var intake = record(MACROS, TODAY).value();
        var corrected = new Macros(35, 55, 12);

        var result = intake.correct(corrected, Optional.empty(), NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(intake.macros()).isEqualTo(corrected);
        assertThat(intake.note()).isEmpty();
        assertThat(intake.pendingEvents()).contains(new IntakeCorrectedEvent(ID, NOW));
    }

    @Test
    void shouldKeepTheDayWhenTheIntakeIsCorrected() {
        var intake = record(MACROS, TODAY.minusDays(1)).value();

        intake.correct(new Macros(35, 55, 12), NOTE, NOW);

        assertThat(intake.consumedOn()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void shouldFailWhenTheCorrectionLeavesNoMacros() {
        var intake = record(MACROS, TODAY).value();

        var result = intake.correct(new Macros(0, 0, 0), NOTE, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(IntakeErrors.MACROS_REQUIRED);
        assertThat(intake.macros()).isEqualTo(MACROS);
    }

    @Test
    void shouldRaiseItsEventWhenDeleted() {
        var intake = record(MACROS, TODAY).value();

        var result = intake.delete(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(intake.pendingEvents()).contains(new IntakeDeletedEvent(ID, NOW));
    }

    @Test
    void shouldRehydrateWithoutRaisingEvents() {
        var intake = Intake.rehydrate(ID, MACROS, Optional.empty(), TODAY, NOW);

        assertThat(intake.macros()).isEqualTo(MACROS);
        assertThat(intake.pendingEvents()).isEmpty();
    }

    private static Result<Intake> record(Macros macros, LocalDate consumedOn) {
        return Intake.record(ID, macros, NOTE, consumedOn, TODAY, NOW);
    }
}
