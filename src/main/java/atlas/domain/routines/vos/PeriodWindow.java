package atlas.domain.routines.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.time.LocalDate;
import java.util.stream.Stream;

public record PeriodWindow(LocalDate start, LocalDate end) implements ValueObject {

    public PeriodWindow {
        ObjectGuard.notNull(start, "start");
        ObjectGuard.notNull(end, "end");

        if (!end.isAfter(start)) {
            throw GuardException.forParameter("end", "must be after 'start'");
        }
    }

    public static PeriodWindow of(LocalDate start, LocalDate end) {
        return new PeriodWindow(start, end);
    }

    public boolean contains(LocalDate day) {
        return !day.isBefore(start) && day.isBefore(end);
    }

    public boolean isClosedOn(LocalDate now) {
        return !end.isAfter(now);
    }

    public Stream<LocalDate> days() {
        return start.datesUntil(end);
    }
}
