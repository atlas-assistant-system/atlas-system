package atlas.application.routines.queries.getroutine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.support.builders.RoutineFixtures;
import java.time.DayOfWeek;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetRoutineQueryHandlerTest {

    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;

    private final RoutineReadModel routines = mock(RoutineReadModel.class);
    private final GetRoutineQueryHandler handler = new GetRoutineQueryHandler(routines);

    @Test
    void shouldReturnTheDetailOfARoutine() {
        when(routines.find(ID))
            .thenReturn(Optional.of(RoutineFixtures.daily(2, DayOfWeek.MONDAY, DayOfWeek.FRIDAY)));

        var result = handler.handle(new GetRoutineQuery(ID));

        assertThat(result.value().period()).isEqualTo("DAY");
        assertThat(result.value().activeDays()).containsExactly("MONDAY", "FRIDAY");
        assertThat(result.value().target()).isEqualByComparingTo("2");
    }

    @Test
    void shouldSortTheDaysOfMonthOfAMonthlyRoutine() {
        when(routines.find(ID)).thenReturn(Optional.of(RoutineFixtures.monthlyOnDays(1, 15, 1)));

        assertThat(handler.handle(new GetRoutineQuery(ID)).value().daysOfMonth()).containsExactly(1, 15);
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.find(ID)).thenReturn(Optional.empty());

        assertThat(handler.handle(new GetRoutineQuery(ID)).error()).isEqualTo(RoutineErrors.notFound(ID));
    }
}
