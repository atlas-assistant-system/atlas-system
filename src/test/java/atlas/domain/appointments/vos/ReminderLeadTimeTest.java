package atlas.domain.appointments.vos;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.appointments.AppointmentErrors;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReminderLeadTimeTest {

    @ParameterizedTest
    @ValueSource(ints = {ReminderLeadTime.MIN_MINUTES, 15, 1440, ReminderLeadTime.MAX_MINUTES})
    void shouldCreateLeadTimeWhenMinutesAreInRange(int minutes) {
        var result = ReminderLeadTime.create(minutes);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().value()).isEqualTo(minutes);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, ReminderLeadTime.MAX_MINUTES + 1})
    void shouldFailWhenMinutesAreOutOfRange(int minutes) {
        var result = ReminderLeadTime.create(minutes);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_REMINDER_LEAD_TIME);
    }

    @Test
    void shouldConvertToDuration() {
        assertThat(ReminderLeadTime.create(90).value().toDuration()).isEqualTo(Duration.ofMinutes(90));
    }

    @Test
    void shouldComputeTriggerTimeBeforeTheSlotStarts() {
        var slot = TimeSlot.of(LocalDateTime.of(2026, 8, 17, 10, 0), LocalDateTime.of(2026, 8, 17, 11, 0));
        var leadTime = ReminderLeadTime.create(30).value();

        assertThat(leadTime.triggerTimeFor(slot)).isEqualTo(LocalDateTime.of(2026, 8, 17, 9, 30));
    }
}
