package atlas.application.economy.commands.correctmovement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.economy.ports.EconomyUnitOfWork;
import atlas.application.economy.ports.MovementRepository;
import atlas.domain.economy.Movement;
import atlas.domain.economy.MovementErrors;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.enums.Category;
import atlas.domain.economy.enums.MovementKind;
import atlas.domain.economy.events.MovementCorrectedEvent;
import atlas.domain.economy.vos.Money;
import atlas.domain.economy.vos.MovementNote;
import atlas.support.builders.UnitOfWorkStub;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorrectMovementCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final MovementId ID = MovementId.of(7);

    private final EconomyUnitOfWork unitOfWork = mock(EconomyUnitOfWork.class);
    private final MovementRepository movements = mock(MovementRepository.class);
    private final CorrectMovementCommandHandler handler =
        new CorrectMovementCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, movements);
    }

    @Test
    void shouldCorrectTheMovementAndReturnIt() {
        var movement = anExpense();
        when(movements.get(ID)).thenReturn(Optional.of(movement));

        var result = handler.handle(
            new CorrectMovementCommand(ID, new BigDecimal("20.00"), "comida", TODAY.minusDays(1)));

        assertThat(result.value().amount()).isEqualByComparingTo("20.00");
        assertThat(result.value().note()).isEqualTo("comida");
        assertThat(result.value().occurredOn()).isEqualTo(TODAY.minusDays(1));
        assertThat(movement.pendingEvents()).contains(new MovementCorrectedEvent(ID, NOW));

        verify(movements).update(movement);
    }

    @Test
    void shouldFailWhenTheMovementDoesNotExist() {
        when(movements.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new CorrectMovementCommand(ID, new BigDecimal("20.00"), null, TODAY));

        assertThat(result.error()).isEqualTo(MovementErrors.notFound(ID));
        verify(movements, never()).update(any());
    }

    @Test
    void shouldFailWhenTheCorrectedAmountIsNotPositive() {
        var result = handler.handle(new CorrectMovementCommand(ID, BigDecimal.ZERO, null, TODAY));

        assertThat(result.error()).isEqualTo(MovementErrors.AMOUNT_MUST_BE_POSITIVE);
        verify(movements, never()).update(any());
    }

    @Test
    void shouldFailWhenTheCorrectionMovesTheDateIntoTheFuture() {
        when(movements.get(ID)).thenReturn(Optional.of(anExpense()));

        var result = handler.handle(
            new CorrectMovementCommand(ID, new BigDecimal("20.00"), null, TODAY.plusDays(1)));

        assertThat(result.error()).isEqualTo(MovementErrors.CANNOT_BE_DATED_IN_THE_FUTURE);
        verify(movements, never()).update(any());
    }

    private static Movement anExpense() {
        return Movement.rehydrate(
            ID, MovementKind.EXPENSE, Money.ofCents(1250).value(), Category.FOOD,
            MovementNote.create("cena").value(), TODAY, NOW);
    }
}
