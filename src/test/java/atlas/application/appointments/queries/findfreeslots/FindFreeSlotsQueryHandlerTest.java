package atlas.application.appointments.queries.findfreeslots;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.services.AppointmentAvailability;
import atlas.domain.appointments.vos.BookedSlot;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FindFreeSlotsQueryHandlerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 17);
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime SIX_PM = LocalTime.of(18, 0);

    private final AppointmentReadModel appointments = mock(AppointmentReadModel.class);
    private final FindFreeSlotsQueryHandler handler =
        new FindFreeSlotsQueryHandler(appointments, new AppointmentAvailability());

    @BeforeEach
    void returnNoBookingsByDefault() {
        when(appointments.findBookedSlots(any(), any())).thenReturn(List.of());
    }

    @Test
    void shouldFailWhenMinimumDurationIsNotPositive() {
        var result = handler.handle(new FindFreeSlotsQuery(DAY, NINE, SIX_PM, 0));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("General.InvalidValue");
        verify(appointments, never()).findBookedSlots(any(), any());
    }

    @Test
    void shouldFailWhenTheSearchWindowIsInvalid() {
        var result = handler.handle(new FindFreeSlotsQuery(DAY, NINE, NINE, 60));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_TIME_SLOT);
    }

    @Test
    void shouldReturnTheWholeWindowWhenNothingIsBooked() {
        var result = handler.handle(new FindFreeSlotsQuery(DAY, NINE, SIX_PM, 60));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).singleElement().satisfies(slot -> {
            assertThat(slot.start()).isEqualTo(DAY.atTime(NINE));
            assertThat(slot.end()).isEqualTo(DAY.atTime(SIX_PM));
        });
    }

    @Test
    void shouldReturnTheGapsAroundABooking() {
        when(appointments.findBookedSlots(any(), any())).thenReturn(List.of(BookedSlot.of(
            AppointmentId.of(2),
            TimeSlot.of(LocalDateTime.of(2026, 8, 17, 10, 0), LocalDateTime.of(2026, 8, 17, 11, 0)))));

        var result = handler.handle(new FindFreeSlotsQuery(DAY, NINE, SIX_PM, 60));

        assertThat(result.value()).hasSize(2);
        assertThat(result.value().getFirst().end()).isEqualTo(LocalDateTime.of(2026, 8, 17, 10, 0));
        assertThat(result.value().getLast().start()).isEqualTo(LocalDateTime.of(2026, 8, 17, 11, 0));
    }
}
