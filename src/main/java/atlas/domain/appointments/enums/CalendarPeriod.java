package atlas.domain.appointments.enums;

import atlas.domain.appointments.vos.TimeSlot;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum CalendarPeriod {

    DAY {

        @Override
        public LocalDate startOf(LocalDate anchor) {
            return anchor;
        }

        @Override
        public LocalDate next(LocalDate anchor) {
            return startOf(anchor).plusDays(1);
        }

        @Override
        public LocalDate previous(LocalDate anchor) {
            return startOf(anchor).minusDays(1);
        }
    },
    WEEK {

        @Override
        public LocalDate startOf(LocalDate anchor) {
            return anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        @Override
        public LocalDate next(LocalDate anchor) {
            return startOf(anchor).plusWeeks(1);
        }

        @Override
        public LocalDate previous(LocalDate anchor) {
            return startOf(anchor).minusWeeks(1);
        }
    },
    MONTH {

        @Override
        public LocalDate startOf(LocalDate anchor) {
            return anchor.withDayOfMonth(1);
        }

        @Override
        public LocalDate next(LocalDate anchor) {
            return startOf(anchor).plusMonths(1);
        }

        @Override
        public LocalDate previous(LocalDate anchor) {
            return startOf(anchor).minusMonths(1);
        }
    },
    YEAR {

        @Override
        public LocalDate startOf(LocalDate anchor) {
            return anchor.withDayOfYear(1);
        }

        @Override
        public LocalDate next(LocalDate anchor) {
            return startOf(anchor).plusYears(1);
        }

        @Override
        public LocalDate previous(LocalDate anchor) {
            return startOf(anchor).minusYears(1);
        }
    };

    public abstract LocalDate startOf(LocalDate anchor);

    public abstract LocalDate next(LocalDate anchor);

    public abstract LocalDate previous(LocalDate anchor);

    public TimeSlot windowFor(LocalDate anchor) {
        return TimeSlot.of(startOf(anchor).atStartOfDay(), next(anchor).atStartOfDay());
    }
}
