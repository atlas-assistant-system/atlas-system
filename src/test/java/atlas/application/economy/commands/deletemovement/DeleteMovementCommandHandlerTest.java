package atlas.application.economy.commands.deletemovement;

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
import atlas.domain.economy.events.MovementDeletedEvent;
import atlas.domain.economy.vos.Money;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteMovementCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final MovementId ID = MovementId.of(7);

    private final MovementUnitOfWork unitOfWork = mock(MovementUnitOfWork.class);
    private final MovementRepository movements = mock(MovementRepository.class);
    private final DeleteMovementCommandHandler handler =
        new DeleteMovementCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, movements);
    }

    @Test
    void shouldRemoveTheMovementAndAnnounceIt() {
        var movement = anExpense();
        when(movements.get(ID)).thenReturn(Optional.of(movement));

        var result = handler.handle(new DeleteMovementCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(movement.pendingEvents()).containsExactly(new MovementDeletedEvent(ID, NOW));

        verify(movements).delete(movement);
    }

    @Test
    void shouldFailWhenTheMovementDoesNotExist() {
        when(movements.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new DeleteMovementCommand(ID));

        assertThat(result.error()).isEqualTo(MovementErrors.notFound(ID));
        verify(movements, never()).delete(any());
    }

    private static Movement anExpense() {
        return Movement.rehydrate(
            ID, MovementKind.EXPENSE, Money.ofCents(1250).value(), Category.FOOD,
            Optional.empty(), TODAY, NOW);
    }
}
