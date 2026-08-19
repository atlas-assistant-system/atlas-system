package atlas.presentation.appointments.web;

import atlas.domain.sharedkernel.exceptions.FormatException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public static int integer(Map<String, Object> body, String field) {
        if (!(body.get(field) instanceof Number number)) {
            throw new FormatException("'" + field + "' must be a number.");
        }

        return number.intValue();
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

    public static List<Integer> integers(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> values)) {
            throw new FormatException("'" + field + "' must be an array of numbers.");
        }

        return values.stream().map(element -> {
            if (!(element instanceof Number number)) {
                throw new FormatException("'" + field + "' must be an array of numbers.");
            }

            return number.intValue();
        }).toList();
    }

    public static LocalDateTime dateTime(Map<String, Object> body, String field) {
        return parseDateTime(text(body, field), field);
    }

    public static LocalDateTime parseDateTime(String value, String name) {
        if (value == null) {
            throw new FormatException("'" + name + "' is required.");
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new FormatException("'" + name + "' must be an ISO date-time, like 2026-08-17T10:00.");
        }
    }

    public static LocalDate parseDate(String value, String name) {
        if (value == null) {
            throw new FormatException("'" + name + "' is required.");
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new FormatException("'" + name + "' must be an ISO date, like 2026-08-17.");
        }
    }

    public static LocalTime parseTime(String value, String name) {
        if (value == null) {
            throw new FormatException("'" + name + "' is required.");
        }

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new FormatException("'" + name + "' must be a time, like 09:00.");
        }
    }

    public static Year parseYear(String value, String name) {
        if (value == null) {
            throw new FormatException("'" + name + "' is required.");
        }

        try {
            return Year.parse(value);
        } catch (DateTimeParseException e) {
            throw new FormatException("'" + name + "' must be a year, like 2026.");
        }
    }

    public static YearMonth parseYearMonth(String value, String name) {
        if (value == null) {
            throw new FormatException("'" + name + "' is required.");
        }

        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new FormatException("'" + name + "' must be a month, like 2026-08.");
        }
    }

    public static int parseInteger(Optional<String> value, String name, int fallback) {
        if (value.isEmpty()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.get());
        } catch (NumberFormatException e) {
            throw new FormatException("'" + name + "' must be a whole number.");
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
}
