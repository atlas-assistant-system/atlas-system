package atlas.presentation.nutrition.web;

import atlas.domain.sharedkernel.exceptions.FormatException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

public final class Values {

    private Values() {}

    public static String text(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return null;
        }

        if (!(value instanceof String content)) {
            throw new FormatException("'" + field + "' must be a string.");
        }

        return content;
    }

    public static BigDecimal kilograms(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            throw new FormatException("'" + field + "' is required.");
        }

        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new FormatException("'" + field + "' must be a weight in kilograms, like \"78.4\".");
        }
    }

    public static int grams(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new FormatException("'" + field + "' must be a whole number of grams.");
        }
    }

    public static LocalDate date(Map<String, Object> body, String field) {
        var value = text(body, field);

        return value == null ? null : parseDate(value, field);
    }

    public static LocalDate parseDate(String value, String name) {
        if (value == null) {
            throw new FormatException("'" + name + "' is required.");
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new FormatException("'" + name + "' must be an ISO date, like 2026-08-22.");
        }
    }

    public static Integer parseLimit(Optional<String> value) {
        return value.map(limit -> {
            try {
                return Integer.valueOf(limit.trim());
            } catch (NumberFormatException e) {
                throw new FormatException("'limit' must be a whole number.");
            }
        }).orElse(null);
    }

    public static int calories(Map<String, Object> body) {
        var value = body.get("calories");
        if (value == null) {
            throw new FormatException("'calories' is required.");
        }

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new FormatException("'calories' must be a whole number of kcal.");
        }
    }
}
