package atlas.application.appointments.commands.addreminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentRepository;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.application.appointments.ports.ReminderIdGenerator;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddReminderCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final AppointmentId ID = AppointmentId.of(1);
    private static final TimeSlot FUTURE_SLOT =
        TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0));

    private final AppointmentUnitOfWork unitOfWork = mock(AppointmentUnitOfWork.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final ReminderIdGenerator reminderIds = mock(ReminderIdGenerator.class);
    private final AddReminderCommandHandler handler =
        new AddReminderCommandHandler(unitOfWork, reminderIds, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        when(unitOfWork.appointments()).thenReturn(appointments);
        when(unitOfWork.execute(any())).thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
        when(reminderIds.next()).thenReturn(ReminderId.of(new UUID(0, 99)));
    }

    @Test
    void shouldAddReminderWhenAppointmentIsUpcoming() {
        var appointment = scheduled();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new AddReminderCommand(ID, 15));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().reminders()).hasSize(1);
        verify(appointments).update(appointment);
    }

    @Test
    void shouldFailWhenLeadTimeIsOutOfRange() {
        var result = handler.handle(new AddReminderCommand(ID, 0));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_REMINDER_LEAD_TIME);
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenAppointmentDoesNotExist() {
        when(appointments.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new AddReminderCommand(ID, 15));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.NotFound");
    }

    @Test
    void shouldFailWhenLeadTimeIsAlreadyUsed() {
        var appointment = scheduled();
        appointment.addReminder(ReminderId.of(new UUID(0, 1)), leadTime(15), NOW_LOCAL, NOW);
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new AddReminderCommand(ID, 15));

        assertThat(result.error()).isEqualTo(AppointmentErrors.DUPLICATE_REMINDER_LEAD_TIME);
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenAppointmentAlreadyHappened() {
        var appointment = Appointment.rehydrate(
            ID, title("Dentista"), Optional.empty(),
            TimeSlot.of(LocalDateTime.of(2026, 8, 17, 6, 0), LocalDateTime.of(2026, 8, 17, 7, 0)),
            AppointmentStatus.SCHEDULED, List.of());
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new AddReminderCommand(ID, 15));

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_ADD_REMINDER_TO_PAST_APPOINTMENT);
    }

    @Test
    void shouldFailWhenAppointmentAlreadyHasTheMaximumOfReminders() {
        var appointment = scheduled();
        for (var i = 1; i <= Appointment.MAX_REMINDERS; i++) {
            appointment.addReminder(ReminderId.of(new UUID(0, i)), leadTime(i), NOW_LOCAL, NOW);
        }
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new AddReminderCommand(ID, 120));

        assertThat(result.error()).isEqualTo(AppointmentErrors.TOO_MANY_REMINDERS);
        verify(appointments, never()).update(any());
    }

    private static Appointment scheduled() {
        return Appointment.schedule(ID, title("Dentista"), Optional.empty(), FUTURE_SLOT, NOW_LOCAL, NOW).value();
    }

    private static AppointmentTitle title(String value) {
        return AppointmentTitle.create(value).value();
    }

    private static ReminderLeadTime leadTime(int minutes) {
        return ReminderLeadTime.create(minutes).value();
    }
}
