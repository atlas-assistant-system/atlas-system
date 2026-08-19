package atlas.application.appointments.queries.getappointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentRepository;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetAppointmentQueryHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final AppointmentId ID = AppointmentId.of(1);

    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final GetAppointmentQueryHandler handler = new GetAppointmentQueryHandler(appointments);

    @Test
    void shouldReturnTheAppointmentWithItsReminders() {
        when(appointments.get(ID)).thenReturn(Optional.of(appointmentWithReminder()));

        var result = handler.handle(new GetAppointmentQuery(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("A00000001");
        assertThat(result.value().title()).isEqualTo("Dentista");
        assertThat(result.value().status()).isEqualTo("SCHEDULED");
        assertThat(result.value().reminders()).hasSize(1);
    }

    @Test
    void shouldFailWhenAppointmentDoesNotExist() {
        when(appointments.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new GetAppointmentQuery(ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.NotFound");
    }

    private static Appointment appointmentWithReminder() {
        var appointment = Appointment.schedule(
            ID, AppointmentTitle.create("Dentista").value(), Optional.empty(),
            TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0)),
            NOW_LOCAL, NOW).value();
        appointment.addReminder(ReminderId.of(new UUID(0, 1)), ReminderLeadTime.create(15).value(), NOW_LOCAL, NOW);

        return appointment;
    }
}
