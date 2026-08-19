package atlas.application.routines.queries.listroutines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.routines.dto.RoutineSummaryDto;
import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.vos.Schedule;
import atlas.support.builders.RoutineFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListRoutinesQueryHandlerTest {

    private final RoutineReadModel routines = mock(RoutineReadModel.class);
    private final ListRoutinesQueryHandler handler = new ListRoutinesQueryHandler(routines);

    @Test
    void shouldSortSummariesByName() {
        var weekly = Schedule.over(RecurrencePeriod.WEEK).value();
        when(routines.findAll(false)).thenReturn(List.of(
            RoutineFixtures.routine(RoutineId.of(1), "Zumba", 1, weekly),
            RoutineFixtures.routine(RoutineId.of(2), "Andar", 1, weekly)));

        var result = handler.handle(new ListRoutinesQuery(false));

        assertThat(result.value()).extracting(RoutineSummaryDto::name).containsExactly("Andar", "Zumba");
    }

    @Test
    void shouldAskForArchivedRoutinesOnlyWhenRequested() {
        when(routines.findAll(true)).thenReturn(List.of());

        handler.handle(new ListRoutinesQuery(true));

        verify(routines).findAll(true);
    }

    @Test
    void shouldReturnNothingWhenThereAreNoRoutines() {
        when(routines.findAll(false)).thenReturn(List.of());

        assertThat(handler.handle(new ListRoutinesQuery(false)).value()).isEmpty();
    }
}
