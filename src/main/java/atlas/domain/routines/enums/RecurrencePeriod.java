package atlas.domain.routines.enums;

import atlas.domain.routines.vos.PeriodWindow;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum RecurrencePeriod {

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
    };

    public abstract LocalDate startOf(LocalDate anchor);

    public abstract LocalDate next(LocalDate anchor);

    public abstract LocalDate previous(LocalDate anchor);

    public PeriodWindow windowFor(LocalDate anchor) {
        return PeriodWindow.of(startOf(anchor), next(anchor));
    }
}
