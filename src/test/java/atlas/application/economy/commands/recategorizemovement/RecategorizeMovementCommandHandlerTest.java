package atlas.application.economy.commands.recategorizemovement;

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
import atlas.domain.economy.events.MovementRecategorizedEvent;
import atlas.domain.economy.vos.Money;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecategorizeMovementCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final MovementId ID = MovementId.of(7);

    private final EconomyUnitOfWork unitOfWork = mock(EconomyUnitOfWork.class);
    private final MovementRepository movements = mock(MovementRepository.class);
    private final RecategorizeMovementCommandHandler handler =
        new RecategorizeMovementCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, movements);
    }

    @Test
    void shouldMoveTheMovementToAnotherCategoryOfTheSameKind() {
        var movement = anExpense();
        when(movements.get(ID)).thenReturn(Optional.of(movement));

        var result = handler.handle(new RecategorizeMovementCommand(ID, Category.LEISURE));

        assertThat(result.value().category()).isEqualTo("LEISURE");
        assertThat(result.value().categoryLabel()).isEqualTo("Ocio");
        assertThat(movement.pendingEvents()).contains(new MovementRecategorizedEvent(ID, NOW));

        verify(movements).update(movement);
    }

    @Test
    void shouldFailWhenTheMovementDoesNotExist() {
        when(movements.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new RecategorizeMovementCommand(ID, Category.LEISURE));

        assertThat(result.error()).isEqualTo(MovementErrors.notFound(ID));
        verify(movements, never()).update(any());
    }

    @Test
    void shouldFailWhenTheCategoryBelongsToTheOtherKind() {
        when(movements.get(ID)).thenReturn(Optional.of(anExpense()));

        var result = handler.handle(new RecategorizeMovementCommand(ID, Category.INCOME));

        assertThat(result.error()).isEqualTo(MovementErrors.CATEGORY_DOES_NOT_MATCH_KIND);
        verify(movements, never()).update(any());
    }

    private static Movement anExpense() {
        return Movement.rehydrate(
            ID, MovementKind.EXPENSE, Money.ofCents(1250).value(), Category.FOOD,
            Optional.empty(), TODAY, NOW);
    }
}
