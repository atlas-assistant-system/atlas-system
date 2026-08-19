package atlas.application.routines.commands.changeroutinedetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineRepository;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.events.RoutineDetailsChangedEvent;
import atlas.support.builders.RoutineFixtures;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeRoutineDetailsCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;

    private final RoutineUnitOfWork unitOfWork = mock(RoutineUnitOfWork.class);
    private final RoutineRepository routines = mock(RoutineRepository.class);
    private final ChangeRoutineDetailsCommandHandler handler =
        new ChangeRoutineDetailsCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, routines);
    }

    @Test
    void shouldRenameTheRoutineAndRaiseItsEvent() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        when(routines.get(ID)).thenReturn(Optional.of(routine));

        var result = handler.handle(new ChangeRoutineDetailsCommand(ID, "Correr mas", "Por la manana"));

        assertThat(result.value().name()).isEqualTo("Correr mas");
        assertThat(result.value().description()).isEqualTo("Por la manana");
        assertThat(routine.pendingEvents()).containsExactly(new RoutineDetailsChangedEvent(ID, NOW));
        verify(routines).update(routine);
    }

    @Test
    void shouldDropTheDescriptionWhenNoneIsGiven() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        when(routines.get(ID)).thenReturn(Optional.of(routine));

        var result = handler.handle(new ChangeRoutineDetailsCommand(ID, "Correr", null));

        assertThat(result.value().description()).isNull();
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new ChangeRoutineDetailsCommand(ID, "Correr", null));

        assertThat(result.error()).isEqualTo(RoutineErrors.notFound(ID));
        verify(routines, never()).update(any());
    }

    @Test
    void shouldFailWhenTheNameIsBlank() {
        var result = handler.handle(new ChangeRoutineDetailsCommand(ID, " ", null));

        assertThat(result.error()).isEqualTo(RoutineErrors.NAME_REQUIRED);
        verify(routines, never()).update(any());
    }

    @Test
    void shouldFailWhenTheDescriptionIsTooLong() {
        var result = handler.handle(new ChangeRoutineDetailsCommand(ID, "Correr", "a".repeat(1001)));

        assertThat(result.error()).isEqualTo(RoutineErrors.DESCRIPTION_TOO_LONG);
        verify(routines, never()).update(any());
    }
}
