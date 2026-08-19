package atlas.domain.appointments.entities;

import atlas.domain.appointments.vos.ReminderLeadTime;
import atlas.domain.appointments.vos.TimeSlot;
import atlas.domain.sharedkernel.ddd.Entity;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

public final class Reminder extends Entity<ReminderId> {

    private final ReminderLeadTime leadTime;
    private Instant acknowledgedAt;

    private Reminder(ReminderId id, ReminderLeadTime leadTime, Instant acknowledgedAt) {
        super(id);
        this.leadTime = ObjectGuard.notNull(leadTime, "leadTime");
        this.acknowledgedAt = acknowledgedAt;
    }

    public static Reminder create(ReminderId id, ReminderLeadTime leadTime) {
        return new Reminder(id, leadTime, null);
    }

    public static Reminder rehydrate(ReminderId id, ReminderLeadTime leadTime, Instant acknowledgedAt) {
        return new Reminder(id, leadTime, acknowledgedAt);
    }

    public ReminderLeadTime leadTime() {
        return leadTime;
    }

    public Optional<Instant> acknowledgedAt() {
        return Optional.ofNullable(acknowledgedAt);
    }

    public boolean isAcknowledged() {
        return acknowledgedAt != null;
    }

    public boolean acknowledge(Instant when) {
        if (acknowledgedAt != null) {
            return false;
        }

        this.acknowledgedAt = ObjectGuard.notNull(when, "when");
        return true;
    }

    public LocalDateTime triggerTimeFor(TimeSlot slot) {
        return leadTime.triggerTimeFor(slot);
    }
}
