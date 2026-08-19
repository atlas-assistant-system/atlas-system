package atlas.application.appointments.commands.rescheduleappointment;

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

class RescheduleAppointmentCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final AppointmentId ID = AppointmentId.of(1);
    private static final LocalDateTime NEW_START = LocalDateTime.of(2026, 8, 17, 15, 0);
    private static final LocalDateTime NEW_END = LocalDateTime.of(2026, 8, 17, 16, 0);

    private final AppointmentUnitOfWork unitOfWork = mock(AppointmentUnitOfWork.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final RescheduleAppointmentCommandHandler handler = new RescheduleAppointmentCommandHandler(
        unitOfWork, new AppointmentAvailability(), Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        when(unitOfWork.appointments()).thenReturn(appointments);
        when(unitOfWork.execute(any())).thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
        when(appointments.findActiveInWindow(any())).thenReturn(List.of());
    }

    @Test
    void shouldRescheduleWhenNewSlotIsFreeAndValid() {
        var appointment = scheduled();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new RescheduleAppointmentCommand(ID, NEW_START, NEW_END, false));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().start()).isEqualTo(NEW_START);
        assertThat(result.value().end()).isEqualTo(NEW_END);
        assertThat(result.value().conflictingAppointmentIds()).isEmpty();
        verify(appointments).update(appointment);
    }

    @Test
    void shouldFailWhenNewSlotIsInvalid() {
        var result = handler.handle(new RescheduleAppointmentCommand(ID, NEW_END, NEW_START, false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_TIME_SLOT);
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenAppointmentDoesNotExist() {
        when(appointments.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new RescheduleAppointmentCommand(ID, NEW_START, NEW_END, false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.NotFound");
    }

    @Test
    void shouldFailWhenAppointmentIsCancelled() {
        when(appointments.get(ID)).thenReturn(Optional.of(cancelled()));

        var result = handler.handle(new RescheduleAppointmentCommand(ID, NEW_START, NEW_END, false));

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenNewSlotOverlapsAndOverrideIsNotAllowed() {
        when(appointments.get(ID)).thenReturn(Optional.of(scheduled()));
        when(appointments.findActiveInWindow(any())).thenReturn(List.of(otherAppointment()));

        var result = handler.handle(new RescheduleAppointmentCommand(ID, NEW_START, NEW_END, false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.Overlaps");
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldRescheduleAnywayWhenOverlapIsExplicitlyAllowed() {
        var appointment = scheduled();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));
        when(appointments.findActiveInWindow(any())).thenReturn(List.of(otherAppointment()));

        var result = handler.handle(new RescheduleAppointmentCommand(ID, NEW_START, NEW_END, true));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().conflictingAppointmentIds()).containsExactly("A00000002");
        verify(appointments).update(appointment);
    }

    @Test
    void shouldNotCountTheAppointmentItselfAsAConflict() {
        var appointment = scheduled();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));
        when(appointments.findActiveInWindow(any()))
            .thenReturn(List.of(appointmentWith(ID, NEW_START.plusMinutes(30), NEW_END.plusMinutes(30))));

        var result = handler.handle(new RescheduleAppointmentCommand(ID, NEW_START, NEW_END, false));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().conflictingAppointmentIds()).isEmpty();
    }

    private static Appointment scheduled() {
        return appointmentWith(ID, LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0));
    }

    private static Appointment cancelled() {
        var appointment = scheduled();
        appointment.cancel(NOW_LOCAL, NOW);

        return appointment;
    }

    private static Appointment otherAppointment() {
        return appointmentWith(AppointmentId.of(2), NEW_START, NEW_END);
    }

    private static Appointment appointmentWith(AppointmentId id, LocalDateTime start, LocalDateTime end) {
        return Appointment.schedule(
            id, AppointmentTitle.create("Dentista").value(), Optional.empty(), TimeSlot.of(start, end),
            NOW_LOCAL, NOW).value();
    }
}
