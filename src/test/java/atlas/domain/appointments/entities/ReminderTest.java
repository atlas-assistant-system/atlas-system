package atlas.domain.appointments.entities;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReminderTest {

    private static final ReminderId ID = ReminderId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final Instant SEEN_AT = Instant.parse("2026-08-17T09:45:00Z");
    private static final Instant LATER = Instant.parse("2026-08-17T09:50:00Z");

    @Test
    void shouldStartUnacknowledged() {
        var reminder = Reminder.create(ID, leadTime(15));

        assertThat(reminder.isAcknowledged()).isFalse();
        assertThat(reminder.acknowledgedAt()).isEmpty();
    }

    @Test
    void shouldRecordTheMomentItWasAcknowledged() {
        var reminder = Reminder.create(ID, leadTime(15));

        var changed = reminder.acknowledge(SEEN_AT);

        assertThat(changed).isTrue();
        assertThat(reminder.isAcknowledged()).isTrue();
        assertThat(reminder.acknowledgedAt()).contains(SEEN_AT);
    }

    @Test
    void shouldKeepTheFirstInstantWhenAcknowledgedTwice() {
        var reminder = Reminder.create(ID, leadTime(15));
        reminder.acknowledge(SEEN_AT);

        var changed = reminder.acknowledge(LATER);

        assertThat(changed).isFalse();
        assertThat(reminder.acknowledgedAt()).contains(SEEN_AT);
    }

    @Test
    void shouldRestoreAnAcknowledgedReminderWhenRehydrated() {
        var reminder = Reminder.rehydrate(ID, leadTime(15), SEEN_AT);

        assertThat(reminder.isAcknowledged()).isTrue();
        assertThat(reminder.acknowledgedAt()).contains(SEEN_AT);
        assertThat(reminder.leadTime()).isEqualTo(leadTime(15));
    }

    @Test
    void shouldRestoreAnUnacknowledgedReminderWhenRehydrated() {
        var reminder = Reminder.rehydrate(ID, leadTime(15), null);

        assertThat(reminder.isAcknowledged()).isFalse();
        assertThat(reminder.acknowledgedAt()).isEmpty();
    }

    @Test
    void shouldStillBeAcknowledgeableAfterBeingRehydratedUnacknowledged() {
        var reminder = Reminder.rehydrate(ID, leadTime(15), null);

        assertThat(reminder.acknowledge(SEEN_AT)).isTrue();
        assertThat(reminder.acknowledgedAt()).contains(SEEN_AT);
    }

    @Test
    void shouldComputeItsTriggerTimeFromTheSlot() {
        var reminder = Reminder.create(ID, leadTime(30));
        var slot = TimeSlot.of(LocalDateTime.of(2026, 8, 17, 11, 0), LocalDateTime.of(2026, 8, 17, 12, 0));

        assertThat(reminder.triggerTimeFor(slot)).isEqualTo(LocalDateTime.of(2026, 8, 17, 10, 30));
    }

    @Test
    void shouldBeEqualByIdentityNotByLeadTime() {
        var one = Reminder.create(ID, leadTime(15));
        var other = Reminder.create(ID, leadTime(60));

        assertThat(one).isEqualTo(other);
    }

    private static ReminderLeadTime leadTime(int minutes) {
        return ReminderLeadTime.create(minutes).value();
    }
}
