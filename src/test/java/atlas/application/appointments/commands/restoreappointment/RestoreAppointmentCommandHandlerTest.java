package atlas.application.appointments.commands.restoreappointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.appointments.ports.AppointmentRepository;
import atlas.application.appointments.ports.AppointmentUnitOfWork;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.services.AppointmentAvailability;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RestoreAppointmentCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final AppointmentId ID = AppointmentId.of(1);
    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 17, 11, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 17, 12, 0);

    private final AppointmentUnitOfWork unitOfWork = mock(AppointmentUnitOfWork.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final RestoreAppointmentCommandHandler handler = new RestoreAppointmentCommandHandler(
        unitOfWork, new AppointmentAvailability(), Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        when(unitOfWork.appointments()).thenReturn(appointments);
        when(unitOfWork.execute(any())).thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
        when(appointments.findActiveInWindow(any())).thenReturn(List.of());
    }

    @Test
    void shouldRestoreACancelledAppointmentWhenItsSlotIsStillFree() {
        var appointment = cancelled();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new RestoreAppointmentCommand(ID, false));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().status()).isEqualTo(AppointmentStatus.SCHEDULED.name());
        assertThat(result.value().conflictingAppointmentIds()).isEmpty();
        verify(appointments).update(appointment);
    }

    @Test
    void shouldFailWhenAppointmentDoesNotExist() {
        when(appointments.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new RestoreAppointmentCommand(ID, false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.NotFound");
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenAppointmentIsNotCancelled() {
        when(appointments.get(ID)).thenReturn(Optional.of(scheduled()));

        var result = handler.handle(new RestoreAppointmentCommand(ID, false));

        assertThat(result.error()).isEqualTo(AppointmentErrors.NOT_CANCELLED);
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenTheCancelledSlotAlreadyStarted() {
        when(appointments.get(ID)).thenReturn(Optional.of(cancelled()));
        var afterItStarted = new RestoreAppointmentCommandHandler(
            unitOfWork,
            new AppointmentAvailability(),
            Clock.fixed(Instant.parse("2026-08-17T11:30:00Z"), ZoneOffset.UTC));

        var result = afterItStarted.handle(new RestoreAppointmentCommand(ID, false));

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_RESTORE_PAST_APPOINTMENT);
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenTheSlotWasTakenWhileCancelledAndOverrideIsNotAllowed() {
        when(appointments.get(ID)).thenReturn(Optional.of(cancelled()));
        when(appointments.findActiveInWindow(any())).thenReturn(List.of(otherAppointment()));

        var result = handler.handle(new RestoreAppointmentCommand(ID, false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.Overlaps");
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldRestoreAnywayWhenOverlapIsExplicitlyAllowed() {
        var appointment = cancelled();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));
        when(appointments.findActiveInWindow(any())).thenReturn(List.of(otherAppointment()));

        var result = handler.handle(new RestoreAppointmentCommand(ID, true));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().conflictingAppointmentIds()).containsExactly("A00000002");
        verify(appointments).update(appointment);
    }

    private static Appointment scheduled() {
        return appointmentWith(ID, START, END);
    }

    private static Appointment cancelled() {
        var appointment = scheduled();
        appointment.cancel(NOW_LOCAL, NOW);

        return appointment;
    }

    private static Appointment otherAppointment() {
        return appointmentWith(AppointmentId.of(2), START, END);
    }

    private static Appointment appointmentWith(AppointmentId id, LocalDateTime start, LocalDateTime end) {
        return Appointment.schedule(
            id, AppointmentTitle.create("Dentista").value(), Optional.empty(), TimeSlot.of(start, end),
            NOW_LOCAL, NOW).value();
    }
}
