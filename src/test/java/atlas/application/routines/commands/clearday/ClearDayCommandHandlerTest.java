package atlas.application.routines.commands.clearday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineEntryRepository;
import atlas.application.routines.ports.RoutineRepository;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.events.DayClearedEvent;
import atlas.support.builders.RoutineFixtures;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClearDayCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;
    private static final LocalDate MON_09 = LocalDate.of(2026, 2, 9);

    private final RoutineUnitOfWork unitOfWork = mock(RoutineUnitOfWork.class);
    private final RoutineEntryRepository entries = mock(RoutineEntryRepository.class);
    private final ClearDayCommandHandler handler =
        new ClearDayCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, mock(RoutineRepository.class), entries);
    }

    @Test
    void shouldRemoveTheEntryAndRaiseItsEvent() {
        var entry = RoutineFixtures.entry(MON_09, 1);
        when(entries.find(ID, MON_09)).thenReturn(Optional.of(entry));

        var result = handler.handle(new ClearDayCommand(ID, MON_09));

        assertThat(result.isSuccess()).isTrue();
        assertThat(entry.pendingEvents()).containsExactly(new DayClearedEvent(ID, MON_09, NOW));
        verify(entries).delete(entry);
    }

    @Test
    void shouldFailWhenThereIsNothingLoggedThatDay() {
        when(entries.find(ID, MON_09)).thenReturn(Optional.empty());

        var result = handler.handle(new ClearDayCommand(ID, MON_09));

        assertThat(result.error()).isEqualTo(RoutineErrors.entryNotFound(ID, MON_09));
        verify(entries, never()).delete(any());
    }
}
