package atlas.application.economy.commands.recordmovement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.economy.ports.MovementRepository;
import atlas.application.economy.ports.MovementUnitOfWork;
import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementErrors;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.events.MovementRecordedEvent;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecordMovementCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final MovementId ID = MovementId.of(7);

    private final MovementUnitOfWork unitOfWork = mock(MovementUnitOfWork.class);
    private final MovementRepository movements = mock(MovementRepository.class);
    private final RecordMovementCommandHandler handler =
        new RecordMovementCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, movements);
        when(movements.nextId()).thenReturn(ID);
    }

    @Test
    void shouldPersistTheMovementAndReturnIt() {
        var result = handler.handle(anExpenseOf("12.50"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("M00000007");
        assertThat(result.value().amount()).isEqualByComparingTo("12.50");
        assertThat(result.value().kind()).isEqualTo("EXPENSE");
        assertThat(result.value().category()).isEqualTo("FOOD");
        assertThat(result.value().note()).isEqualTo("cena");
        assertThat(result.value().occurredOn()).isEqualTo(TODAY);

        verify(movements).create(any(Movement.class));
    }

    @Test
    void shouldRegisterTheRecordedEvent() {
        handler.handle(anExpenseOf("12.50"));

        var saved = ArgumentCaptor.forClass(Movement.class);
        verify(movements).create(saved.capture());

        assertThat(saved.getValue().pendingEvents()).containsExactly(new MovementRecordedEvent(ID, NOW));
    }

    @Test
    void shouldDateTheMovementTodayWhenNoDateIsGiven() {
        var command = new RecordMovementCommand(
            MovementKind.EXPENSE, new BigDecimal("12.50"), Category.FOOD, "cena", null);

        assertThat(handler.handle(command).value().occurredOn()).isEqualTo(TODAY);
    }

    @Test
    void shouldFailWhenTheAmountIsNotPositive() {
        var command = new RecordMovementCommand(
            MovementKind.EXPENSE, BigDecimal.ZERO, Category.FOOD, "cena", TODAY);

        var result = handler.handle(command);

        assertThat(result.error()).isEqualTo(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
        verify(movements, never()).create(any());
    }

    @Test
    void shouldFailWhenTheNoteIsTooLong() {
        var command = new RecordMovementCommand(
            MovementKind.EXPENSE, new BigDecimal("12.50"), Category.FOOD, "x".repeat(200), TODAY);

        assertThat(handler.handle(command).error()).isEqualTo(MovementErrors.NOTE_TOO_LONG);
        verify(movements, never()).create(any());
    }

    @Test
    void shouldFailWhenTheCategoryBelongsToTheOtherKind() {
        var command = new RecordMovementCommand(
            MovementKind.EXPENSE, new BigDecimal("12.50"), Category.INCOME, null, TODAY);

        assertThat(handler.handle(command).error()).isEqualTo(MovementErrors.CATEGORY_DOES_NOT_MATCH_KIND);
        verify(movements, never()).create(any());
    }

    @Test
    void shouldFailWhenTheMovementIsDatedInTheFuture() {
        var command = new RecordMovementCommand(
            MovementKind.EXPENSE, new BigDecimal("12.50"), Category.FOOD, null, TODAY.plusDays(1));

        assertThat(handler.handle(command).error()).isEqualTo(MovementErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        verify(movements, never()).create(any());
    }

    private static RecordMovementCommand anExpenseOf(String amount) {
        return new RecordMovementCommand(
            MovementKind.EXPENSE, new BigDecimal(amount), Category.FOOD, "cena", TODAY);
    }
}
