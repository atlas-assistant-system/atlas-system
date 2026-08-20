package atlas.application.economy.queries;

import java.time.LocalDate;

public record Period(LocalDate from, LocalDate to) {

    public static Period of(LocalDate from, LocalDate to, LocalDate today) {
        return new Period(
            from == null ? today.withDayOfMonth(1) : from,
            to == null ? today.withDayOfMonth(today.lengthOfMonth()) : to);
    }
}
