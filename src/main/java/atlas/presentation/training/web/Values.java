package atlas.presentation.training.web;

import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.training.enums.Metric;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Los valores tal y como llegan por HTTP en este contexto. No se comparte con los demas:
 * aqui "load" son kilos con decimales y "metric" es un enum del dominio; en routines
 * "weekdays" es otra cosa y en economy "amount" es euros.
 */
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

    public static Metric metric(Map<String, Object> body, String field) {
        var value = text(body, field);
        if (value == null || value.isBlank()) {
            throw new FormatException("'" + field + "' is required.");
        }

        try {
            return Metric.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new FormatException(
                "'" + field + "' must be one of LOAD, REPS, TIME or DISTANCE.");
        }
    }

    /** La carga viaja en kilos con decimales: "72.5" son los discos de 2,5. */
    public static BigDecimal kilograms(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new FormatException("'" + field + "' must be a load in kilograms, like \"72.5\".");
        }
    }

    public static int count(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new FormatException("'" + field + "' must be a whole number.");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> rows(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> items)) {
            throw new FormatException("'" + field + "' must be a list.");
        }

        for (var item : items) {
            if (!(item instanceof Map<?, ?>)) {
                throw new FormatException("'" + field + "' must be a list of objects.");
            }
        }

        return (List<Map<String, Object>>) items;
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

    public static boolean flag(Optional<String> value) {
        return value.map(raw -> "true".equalsIgnoreCase(raw.trim())).orElse(false);
    }
}
