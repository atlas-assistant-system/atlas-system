package atlas.application.appointments.commands.deleteappointment;

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
import atlas.domain.appointments.events.AppointmentDeletedEvent;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteAppointmentCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final AppointmentId ID = AppointmentId.of(1);

    private final AppointmentUnitOfWork unitOfWork = mock(AppointmentUnitOfWork.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final DeleteAppointmentCommandHandler handler =
        new DeleteAppointmentCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        when(unitOfWork.appointments()).thenReturn(appointments);
        when(unitOfWork.execute(any())).thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
    }

    @Test
    void shouldDeleteTheAppointmentAndAnnounceIt() {
        var appointment = scheduled();
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new DeleteAppointmentCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        verify(appointments).delete(appointment);
        assertThat(appointment.pendingEvents())
            .last()
            .isInstanceOfSatisfying(AppointmentDeletedEvent.class, event -> {
                assertThat(event.appointmentId()).isEqualTo(ID);
                assertThat(event.occurredOn()).isEqualTo(NOW);
            });
    }

    @Test
    void shouldDeleteACancelledAppointmentToo() {
        var appointment = scheduled();
        appointment.cancel(NOW_LOCAL, NOW);
        when(appointments.get(ID)).thenReturn(Optional.of(appointment));

        var result = handler.handle(new DeleteAppointmentCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        verify(appointments).delete(appointment);
    }

    @Test
    void shouldFailWhenAppointmentDoesNotExist() {
        when(appointments.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new DeleteAppointmentCommand(ID));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.NotFound");
        verify(appointments, never()).delete(any());
    }

    private static Appointment scheduled() {
        return Appointment.schedule(
            ID,
            AppointmentTitle.create("Dentista").value(),
            Optional.empty(),
            TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0)),
            NOW_LOCAL,
            NOW).value();
    }
}
