package atlas.presentation.routines.web;

import atlas.domain.sharedkernel.exceptions.FormatException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Values {

    private Values() {}

    public static String text(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return null;
        }

        if (!(value instanceof String text)) {
            throw new FormatException("'" + field + "' must be a string.");
        }

        return text;
    }

    public static BigDecimal decimal(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }

        if (value instanceof String text) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException e) {
                throw new FormatException("'" + field + "' must be a number.");
            }
        }

        throw new FormatException("'" + field + "' must be a number.");
    }

    public static boolean flag(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return false;
        }

        if (!(value instanceof Boolean flag)) {
            throw new FormatException("'" + field + "' must be a boolean.");
        }

        return flag;
    }

    public static Set<DayOfWeek> weekdays(Map<String, Object> body, String field) {
        var days = new LinkedHashSet<DayOfWeek>();

        for (var element : list(body, field)) {
            if (!(element instanceof String text)) {
                throw new FormatException("'" + field + "' must be an array of weekday names.");
            }

            days.add(parseEnum(text, DayOfWeek.class, field));
        }

        return days;
    }

    public static Set<Integer> daysOfMonth(Map<String, Object> body, String field) {
        var days = new LinkedHashSet<Integer>();

        for (var element : list(body, field)) {
            if (!(element instanceof Number number)) {
                throw new FormatException("'" + field + "' must be an array of numbers.");
            }

            days.add(number.intValue());
        }

        return days;
    }

    public static LocalDate date(Map<String, Object> body, String field) {
        return parseDate(text(body, field), field);
    }

    public static LocalDate parseDate(String value, String name) {
        if (value == null) {
            throw new FormatException("'" + name + "' is required.");
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new FormatException("'" + name + "' must be an ISO date, like 2026-02-14.");
        }
    }

    public static boolean parseFlag(Optional<String> value) {
        return value.map(Boolean::parseBoolean).orElse(false);
    }

    public static <E extends Enum<E>> E parseEnum(String value, Class<E> type, String name) {
        if (value == null) {
            throw new FormatException("'" + name + "' is required.");
        }

        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new FormatException("'" + name + "' must be one of: " + List.of(type.getEnumConstants()) + ".");
        }
    }

    private static List<?> list(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> values)) {
            throw new FormatException("'" + field + "' must be an array.");
        }

        return values;
    }
}
