package atlas.application.appointments.queries.getupcomingappointments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentReadModel;
import atlas.application.appointments.ports.AppointmentSummary;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetUpcomingAppointmentsQueryHandlerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 8, 0);

    private final AppointmentReadModel appointments = mock(AppointmentReadModel.class);
    private final GetUpcomingAppointmentsQueryHandler handler = new GetUpcomingAppointmentsQueryHandler(appointments);

    @Test
    void shouldFailWhenLimitIsNotPositive() {
        var result = handler.handle(new GetUpcomingAppointmentsQuery(NOW, 0));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("General.InvalidValue");
        verify(appointments, never()).findUpcoming(any(), anyInt());
    }

    @Test
    void shouldMapTheUpcomingAppointmentsToDtos() {
        when(appointments.findUpcoming(NOW, 5)).thenReturn(List.of(summary()));

        var result = handler.handle(new GetUpcomingAppointmentsQuery(NOW, 5));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("A00000001");
            assertThat(dto.title()).isEqualTo("Dentista");
            assertThat(dto.status()).isEqualTo("SCHEDULED");
        });
    }

    @Test
    void shouldForwardTheMomentAndTheLimitVerbatim() {
        when(appointments.findUpcoming(NOW, 3)).thenReturn(List.of());

        handler.handle(new GetUpcomingAppointmentsQuery(NOW, 3));

        verify(appointments).findUpcoming(NOW, 3);
    }

    private static AppointmentSummary summary() {
        return new AppointmentSummary(
            AppointmentId.of(1),
            AppointmentTitle.create("Dentista").value(),
            TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0)),
            AppointmentStatus.SCHEDULED);
    }
}
