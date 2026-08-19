package atlas.domain.appointments.vos;

import atlas.domain.appointments.AppointmentErrors;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Duration;
import java.time.LocalDateTime;

public record TimeSlot(LocalDateTime start, LocalDateTime end) implements ValueObject {

    public TimeSlot {
        ObjectGuard.notNull(start, "start");
        ObjectGuard.notNull(end, "end");

        if (!end.isAfter(start)) {
            throw GuardException.forParameter("end", "must be after 'start'");
        }
    }

    public static Result<TimeSlot> create(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return Result.failure(AppointmentErrors.TIME_SLOT_REQUIRED);
        }

        if (!end.isAfter(start)) {
            return Result.failure(AppointmentErrors.INVALID_TIME_SLOT);
        }

        return Result.success(new TimeSlot(start, end));
    }

    public static TimeSlot of(LocalDateTime start, LocalDateTime end) {
        return new TimeSlot(start, end);
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean contains(LocalDateTime moment) {
        return !moment.isBefore(start) && moment.isBefore(end);
    }

    public boolean isPast(LocalDateTime now) {
        return !end.isAfter(now);
    }

    public boolean startsInThePast(LocalDateTime now) {
        return start.isBefore(now);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}
