package atlas.application.appointments.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.appointments.ports.AppointmentSummary;
import atlas.application.appointments.ports.DailyAppointmentCount;
import atlas.application.appointments.ports.DueReminder;
import atlas.application.appointments.ports.MonthlyAppointmentCount;
import atlas.domain.appointments.Appointment;
import atlas.domain.appointments.AppointmentId;
import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.appointments.enums.AppointmentStatus;
import atlas.domain.appointments.vos.AppointmentDescription;
import atlas.domain.appointments.vos.AppointmentTitle;
import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentMapperTest {

    private static final Instant OCCURRED_ON = Instant.parse("2026-08-17T08:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 17, 8, 0);
    private static final TimeSlot SLOT =
        TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0));
    private static final ReminderId REMINDER_ID = ReminderId.of(new UUID(0, 1));

    @Test
    void shouldMapEveryAppointmentField() {
        var dto = AppointmentMapper.toDto(appointmentWithReminder());

        assertThat(dto.id()).isEqualTo("A00000001");
        assertThat(dto.title()).isEqualTo("Dentista");
        assertThat(dto.description()).isEqualTo("Llevar radiografia");
        assertThat(dto.start()).isEqualTo(SLOT.start());
        assertThat(dto.end()).isEqualTo(SLOT.end());
        assertThat(dto.status()).isEqualTo("SCHEDULED");
        assertThat(dto.conflictingAppointmentIds()).isEmpty();
    }

    @Test
    void shouldMapRemindersWithTheirAcknowledgement() {
        var appointment = appointmentWithReminder();
        appointment.acknowledgeReminder(REMINDER_ID, OCCURRED_ON);

        var dto = AppointmentMapper.toDto(appointment);

        assertThat(dto.reminders()).singleElement().satisfies(reminder -> {
            assertThat(reminder.id()).isEqualTo(REMINDER_ID.toString());
            assertThat(reminder.leadTimeMinutes()).isEqualTo(15);
            assertThat(reminder.acknowledgedAt()).isEqualTo(OCCURRED_ON);
        });
    }

    @Test
    void shouldLeaveDescriptionNullWhenTheAppointmentHasNone() {
        var appointment = Appointment.schedule(
            AppointmentId.of(1), title("Dentista"), java.util.Optional.empty(), SLOT, NOW, OCCURRED_ON).value();

        assertThat(AppointmentMapper.toDto(appointment).description()).isNull();
    }

    @Test
    void shouldRenderConflictingIdsInTheirPrefixedForm() {
        var dto = AppointmentMapper.toDto(
            appointmentWithReminder(), List.of(AppointmentId.of(2), AppointmentId.of(3)));

        assertThat(dto.conflictingAppointmentIds()).containsExactly("A00000002", "A00000003");
    }

    @Test
    void shouldMapASummaryRow() {
        var summary = new AppointmentSummary(AppointmentId.of(1), title("Dentista"), SLOT,
            AppointmentStatus.CANCELLED);

        var dto = AppointmentMapper.toDto(summary);

        assertThat(dto.id()).isEqualTo("A00000001");
        assertThat(dto.title()).isEqualTo("Dentista");
        assertThat(dto.start()).isEqualTo(SLOT.start());
        assertThat(dto.end()).isEqualTo(SLOT.end());
        assertThat(dto.status()).isEqualTo("CANCELLED");
    }

    @Test
    void shouldMapCountBuckets() {
        assertThat(AppointmentMapper.toDto(new MonthlyAppointmentCount(YearMonth.of(2026, 8), 3)).count())
            .isEqualTo(3);
        assertThat(AppointmentMapper.toDto(new DailyAppointmentCount(LocalDate.of(2026, 8, 17), 2)).count())
            .isEqualTo(2);
    }

    @Test
    void shouldMapAFreeSlot() {
        var dto = AppointmentMapper.toFreeSlotDto(SLOT);

        assertThat(dto.start()).isEqualTo(SLOT.start());
        assertThat(dto.end()).isEqualTo(SLOT.end());
    }

    @Test
    void shouldMapADueReminder() {
        var due = new DueReminder(AppointmentId.of(1), REMINDER_ID, title("Dentista"), SLOT,
            ReminderLeadTime.create(15).value());

        var dto = AppointmentMapper.toDto(due);

        assertThat(dto.appointmentId()).isEqualTo("A00000001");
        assertThat(dto.reminderId()).isEqualTo(REMINDER_ID.toString());
        assertThat(dto.title()).isEqualTo("Dentista");
        assertThat(dto.start()).isEqualTo(SLOT.start());
        assertThat(dto.leadTimeMinutes()).isEqualTo(15);
    }

    private static Appointment appointmentWithReminder() {
        var appointment = Appointment.schedule(
            AppointmentId.of(1),
            title("Dentista"),
            AppointmentDescription.create("Llevar radiografia").value(),
            SLOT,
            NOW,
            OCCURRED_ON).value();
        appointment.addReminder(REMINDER_ID, ReminderLeadTime.create(15).value(), NOW, OCCURRED_ON);

        return appointment;
    }

    private static AppointmentTitle title(String value) {
        return AppointmentTitle.create(value).value();
    }
}
