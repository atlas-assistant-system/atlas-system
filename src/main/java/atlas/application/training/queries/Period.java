package atlas.application.training.queries;

import java.time.LocalDate;

public record Period(LocalDate from, LocalDate to) {

    public static final int DEFAULT_DAYS = 30;

    public static Period of(LocalDate from, LocalDate to, LocalDate today) {
        var end = to == null ? today : to;

        return new Period(from == null ? end.minusDays(DEFAULT_DAYS - 1L) : from, end);
    }
}
