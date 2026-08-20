package atlas.domain.economy;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.events.MovementCorrectedEvent;
import atlas.domain.economy.events.MovementDeletedEvent;
import atlas.domain.economy.events.MovementRecategorizedEvent;
import atlas.domain.economy.events.MovementRecordedEvent;
import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.MovementNote;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MovementTest {

    private static final MovementId ID = MovementId.of(1);
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final Money TWELVE_FIFTY = Money.ofCents(1250).value();

    @Test
    void shouldRecordAnExpenseWithEverythingItWasGiven() {
        var movement = anExpense().value();

        assertThat(movement.id()).isEqualTo(ID);
        assertThat(movement.kind()).isEqualTo(MovementKind.EXPENSE);
        assertThat(movement.amount()).isEqualTo(TWELVE_FIFTY);
        assertThat(movement.category()).isEqualTo(Category.FOOD);
        assertThat(movement.note()).map(MovementNote::value).contains("cena");
        assertThat(movement.occurredOn()).isEqualTo(TODAY);
        assertThat(movement.recordedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRegisterRecordedEventWhenRecorded() {
        var movement = anExpense().value();

        assertThat(movement.pendingEvents())
            .containsExactly(new MovementRecordedEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenTheCategoryBelongsToTheOtherKind() {
        var result = Movement.record(
            ID, MovementKind.EXPENSE, TWELVE_FIFTY, Category.INCOME, Optional.empty(), TODAY, TODAY, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(MovementErrors.CATEGORY_DOES_NOT_MATCH_KIND);
    }

    @Test
    void shouldFailWhenAnIncomeIsLabelledAsSpending() {
        var result = Movement.record(
            ID, MovementKind.INCOME, TWELVE_FIFTY, Category.FOOD, Optional.empty(), TODAY, TODAY, NOW);

        assertThat(result.error()).isEqualTo(MovementErrors.CATEGORY_DOES_NOT_MATCH_KIND);
    }

    @Test
    void shouldFailWhenTheMovementIsDatedInTheFuture() {
        var result = Movement.record(
            ID, MovementKind.EXPENSE, TWELVE_FIFTY, Category.FOOD, Optional.empty(),
            TODAY.plusDays(1), TODAY, NOW);

        assertThat(result.error()).isEqualTo(MovementErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
    }

    @Test
    void shouldAcceptAMovementDatedToday() {
        assertThat(anExpense().isSuccess()).isTrue();
    }

    @Test
    void shouldCorrectAmountNoteAndDate() {
        var movement = anExpense().value();
        movement.clearEvents();
        var corrected = Money.ofCents(2000).value();

        var result = movement.correct(corrected, Optional.empty(), TODAY.minusDays(2), TODAY, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(movement.amount()).isEqualTo(corrected);
        assertThat(movement.note()).isEmpty();
        assertThat(movement.occurredOn()).isEqualTo(TODAY.minusDays(2));
        assertThat(movement.pendingEvents()).containsExactly(new MovementCorrectedEvent(ID, NOW));
    }

    @Test
    void shouldKeepTheOriginalRecordedAtWhenCorrected() {
        var movement = anExpense().value();
        var later = NOW.plusSeconds(3600);

        movement.correct(TWELVE_FIFTY, Optional.empty(), TODAY, TODAY, later);

        assertThat(movement.recordedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldFailWhenACorrectionMovesTheDateIntoTheFuture() {
        var movement = anExpense().value();

        var result = movement.correct(TWELVE_FIFTY, Optional.empty(), TODAY.plusDays(1), TODAY, NOW);

        assertThat(result.error()).isEqualTo(MovementErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        assertThat(movement.occurredOn()).isEqualTo(TODAY);
    }

    @Test
    void shouldRecategorizeWithinTheSameKind() {
        var movement = anExpense().value();
        movement.clearEvents();

        var result = movement.recategorize(Category.LEISURE, NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(movement.category()).isEqualTo(Category.LEISURE);
        assertThat(movement.pendingEvents()).containsExactly(new MovementRecategorizedEvent(ID, NOW));
    }

    @Test
    void shouldFailWhenRecategorizingIntoTheOtherKind() {
        var movement = anExpense().value();

        var result = movement.recategorize(Category.INCOME, NOW);

        assertThat(result.error()).isEqualTo(MovementErrors.CATEGORY_DOES_NOT_MATCH_KIND);
        assertThat(movement.category()).isEqualTo(Category.FOOD);
    }

    @Test
    void shouldRegisterDeletedEventWhenDeleted() {
        var movement = anExpense().value();
        movement.clearEvents();

        var result = movement.delete(NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(movement.pendingEvents()).containsExactly(new MovementDeletedEvent(ID, NOW));
    }

    @Test
    void shouldNotRegisterAnyEventWhenRehydratedFromStorage() {
        var movement = Movement.rehydrate(
            ID, MovementKind.EXPENSE, TWELVE_FIFTY, Category.FOOD, Optional.empty(), TODAY, NOW);

        assertThat(movement.pendingEvents()).isEmpty();
    }

    private static atlas.domain.sharedkernel.results.Result<Movement> anExpense() {
        return Movement.record(
            ID, MovementKind.EXPENSE, TWELVE_FIFTY, Category.FOOD,
            MovementNote.create("cena").value(), TODAY, TODAY, NOW);
    }
}
