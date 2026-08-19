package atlas.domain.appointments.vos;

import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Duration;
import java.time.LocalDateTime;

public record ReminderLeadTime(Integer value) implements SingleValueObject<Integer> {

    public static final int MIN_MINUTES = 1;
    public static final int MAX_MINUTES = 10080;

    public ReminderLeadTime {
        ObjectGuard.notNull(value, "value");
        NumberGuard.inRange(value, MIN_MINUTES, MAX_MINUTES, "value");
    }

    public static Result<ReminderLeadTime> create(int minutes) {
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            return Result.failure(AppointmentErrors.INVALID_REMINDER_LEAD_TIME);
        }

        return Result.success(new ReminderLeadTime(minutes));
    }

    public Duration toDuration() {
        return Duration.ofMinutes(value);
    }

    public LocalDateTime triggerTimeFor(TimeSlot slot) {
        return slot.start().minusMinutes(value);
    }
}
