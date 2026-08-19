package atlas.application.appointments.commands.removereminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentRepository;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoveReminderCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final AppointmentId ID = AppointmentId.of(1);
    private static final ReminderId REMINDER_ID = ReminderId.of(new UUID(0, 1));

    private final AppointmentUnitOfWork unitOfWork = mock(AppointmentUnitOfWork.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final RemoveReminderCommandHandler handler =
        new RemoveReminderCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        when(unitOfWork.appointments()).thenReturn(appointments);
        when(unitOfWork.execute(any())).thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
    }

    @Test
    void shouldRemoveTheReminderWhenItExists() {
        var appointment = withReminder();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new RemoveReminderCommand(ID, REMINDER_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.reminders()).isEmpty();
        verify(appointments).update(appointment);
    }

    @Test
    void shouldFailWhenAppointmentDoesNotExist() {
        when(appointments.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new RemoveReminderCommand(ID, REMINDER_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.NotFound");
    }

    @Test
    void shouldFailWhenReminderIsNotThere() {
        when(appointments.get(ID)).thenReturn(Optional.of(scheduled()));

        var result = handler.handle(new RemoveReminderCommand(ID, REMINDER_ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.ReminderNotFound");
        verify(appointments, never()).update(any());
    }

    private static Appointment scheduled() {
        return Appointment.schedule(
            ID, AppointmentTitle.create("Dentista").value(), Optional.empty(),
            TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0)),
            NOW_LOCAL, NOW).value();
    }

    private static Appointment withReminder() {
        var appointment = scheduled();
        appointment.addReminder(REMINDER_ID, ReminderLeadTime.create(15).value(), NOW_LOCAL, NOW);

        return appointment;
    }
}
