package atlas.application.appointments.commands.cancelappointment;

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

class CancelAppointmentCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final AppointmentId ID = AppointmentId.of(1);
    private static final TimeSlot FUTURE_SLOT =
        TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0));
    private static final TimeSlot PAST_SLOT =
        TimeSlot.of(LocalDateTime.of(2026, 8, 17, 6, 0), LocalDateTime.of(2026, 8, 17, 7, 0));

    private final AppointmentUnitOfWork unitOfWork = mock(AppointmentUnitOfWork.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final CancelAppointmentCommandHandler handler =
        new CancelAppointmentCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        when(unitOfWork.appointments()).thenReturn(appointments);
        when(unitOfWork.execute(any())).thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
    }

    @Test
    void shouldCancelTheAppointmentWhenItIsActive() {
        var appointment = scheduled();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new CancelAppointmentCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointments).update(appointment);
    }

    @Test
    void shouldFailWhenAppointmentDoesNotExist() {
        when(appointments.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new CancelAppointmentCommand(ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.NotFound");
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenAppointmentIsAlreadyCancelled() {
        var appointment = scheduled();
        appointment.cancel(NOW_LOCAL, NOW);
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new CancelAppointmentCommand(ID));

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
        verify(appointments, never()).update(any());
    }

    @Test
    void shouldFailWhenAppointmentAlreadyHappened() {
        var appointment = Appointment.rehydrate(
            ID, title("Dentista"), Optional.empty(), PAST_SLOT, AppointmentStatus.SCHEDULED, List.of());
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new CancelAppointmentCommand(ID));

        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_MODIFY_PAST_APPOINTMENT);
        verify(appointments, never()).update(any());
    }

    private static Appointment scheduled() {
        return Appointment.schedule(ID, title("Dentista"), Optional.empty(), FUTURE_SLOT, NOW_LOCAL, NOW).value();
    }

    private static AppointmentTitle title(String value) {
        return AppointmentTitle.create(value).value();
    }
}
