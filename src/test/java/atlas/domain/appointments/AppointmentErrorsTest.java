package atlas.domain.appointments;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.appointments.entities.ReminderId;
import atlas.domain.sharedkernel.results.ErrorType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentErrorsTest {

    @Test
    void shouldReportTheSingleAppointmentAnOverlapCollidesWith() {
        var error = AppointmentErrors.overlaps(List.of(AppointmentId.of(7)));

        assertThat(error.code()).isEqualTo("Appointment.Overlaps");
        assertThat(error.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(error.message()).contains("A00000007");
    }

    @Test
    void shouldReportEveryAppointmentAnOverlapCollidesWith() {
        var error = AppointmentErrors.overlaps(List.of(AppointmentId.of(7), AppointmentId.of(9)));

        assertThat(error.message()).contains("A00000007", "A00000009");
    }

    @Test
    void shouldNameTheAppointmentThatWasNotFound() {
        var error = AppointmentErrors.notFound(AppointmentId.of(7));

        assertThat(error.code()).isEqualTo("Appointment.NotFound");
        assertThat(error.type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(error.message()).contains("A00000007");
    }

    @Test
    void shouldNameTheReminderThatWasNotFound() {
        var id = ReminderId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        var error = AppointmentErrors.reminderNotFound(id);

        assertThat(error.code()).isEqualTo("Appointment.ReminderNotFound");
        assertThat(error.type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(error.message()).contains("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void shouldClassifyStateConflictsApartFromInvalidInput() {
        assertThat(AppointmentErrors.CANNOT_MODIFY_CANCELLED.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(AppointmentErrors.CANNOT_MODIFY_PAST_APPOINTMENT.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(AppointmentErrors.TOO_MANY_REMINDERS.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(AppointmentErrors.DUPLICATE_REMINDER_LEAD_TIME.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(AppointmentErrors.TITLE_REQUIRED.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(AppointmentErrors.INVALID_TIME_SLOT.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(AppointmentErrors.CANNOT_SCHEDULE_IN_THE_PAST.type()).isEqualTo(ErrorType.VALIDATION);
    }
}
