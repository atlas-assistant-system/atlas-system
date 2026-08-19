package atlas.application.appointments.commands.scheduleappointment;

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
import atlas.domain.appointments.services.AppointmentAvailability;
import atlas.domain.appointments.vos.AppointmentTitle;
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

class ScheduleAppointmentCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime ELEVEN = LocalDateTime.of(2026, 8, 17, 11, 0);
    private static final LocalDateTime TWELVE = LocalDateTime.of(2026, 8, 17, 12, 0);
    private static final AppointmentId NEXT_ID = AppointmentId.of(1);

    private final AppointmentUnitOfWork unitOfWork = mock(AppointmentUnitOfWork.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final ReminderIdGenerator reminderIds = mock(ReminderIdGenerator.class);
    private final ScheduleAppointmentCommandHandler handler = new ScheduleAppointmentCommandHandler(
        unitOfWork, new AppointmentAvailability(), reminderIds, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        when(unitOfWork.appointments()).thenReturn(appointments);
        when(unitOfWork.execute(any())).thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
        when(appointments.nextId()).thenReturn(NEXT_ID);
        when(appointments.findActiveInWindow(any())).thenReturn(List.of());
    }

    @Test
    void shouldPersistAppointmentWhenSlotIsFreeAndValid() {
        var result = handler.handle(command(ELEVEN, TWELVE, List.of(), false));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("A00000001");
        assertThat(result.value().title()).isEqualTo("Dentista");
        assertThat(result.value().status()).isEqualTo("SCHEDULED");
        assertThat(result.value().conflictingAppointmentIds()).isEmpty();
        verify(appointments).create(any(Appointment.class));
    }

    @Test
    void shouldCreateRemindersWhenLeadTimesAreGiven() {
        when(reminderIds.next())
            .thenReturn(ReminderId.of(new UUID(0, 1)), ReminderId.of(new UUID(0, 2)));

        var result = handler.handle(command(ELEVEN, TWELVE, List.of(15, 60), false));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().reminders()).hasSize(2);
    }

    @Test
    void shouldFailWhenTitleIsBlank() {
        var result = handler.handle(new ScheduleAppointmentCommand(" ", null, ELEVEN, TWELVE, List.of(), false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.TITLE_REQUIRED);
        verify(appointments, never()).create(any());
    }

    @Test
    void shouldFailWhenTimeSlotIsInvalid() {
        var result = handler.handle(command(TWELVE, ELEVEN, List.of(), false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_TIME_SLOT);
        verify(appointments, never()).create(any());
    }

    @Test
    void shouldFailWhenSchedulingInThePast() {
        var result = handler.handle(
            command(LocalDateTime.of(2026, 8, 17, 6, 0), LocalDateTime.of(2026, 8, 17, 7, 0), List.of(), false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.CANNOT_SCHEDULE_IN_THE_PAST);
    }

    @Test
    void shouldFailWhenALeadTimeIsInvalid() {
        var result = handler.handle(command(ELEVEN, TWELVE, List.of(0), false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_REMINDER_LEAD_TIME);
    }

    @Test
    void shouldFailWhenSlotOverlapsAndOverrideIsNotAllowed() {
        when(appointments.findActiveInWindow(any())).thenReturn(List.of(existingAppointment()));

        var result = handler.handle(command(ELEVEN, TWELVE, List.of(), false));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Appointment.Overlaps");
        verify(appointments, never()).create(any());
    }

    @Test
    void shouldScheduleAnywayWhenOverlapIsExplicitlyAllowed() {
        when(appointments.findActiveInWindow(any())).thenReturn(List.of(existingAppointment()));

        var result = handler.handle(command(ELEVEN, TWELVE, List.of(), true));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().conflictingAppointmentIds()).containsExactly("A00000002");
        verify(appointments).create(any(Appointment.class));
    }

    private static ScheduleAppointmentCommand command(
        LocalDateTime start, LocalDateTime end, List<Integer> leadTimes, boolean allowOverlap) {
        return new ScheduleAppointmentCommand("Dentista", null, start, end, leadTimes, allowOverlap);
    }

    private static Appointment existingAppointment() {
        return Appointment.schedule(
            AppointmentId.of(2),
            AppointmentTitle.create("Ocupada").value(),
            Optional.empty(),
            TimeSlot.of(ELEVEN, TWELVE),
            LocalDateTime.of(2026, 8, 17, 8, 0),
            NOW).value();
    }
}
