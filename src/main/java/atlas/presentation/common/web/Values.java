package atlas.presentation.common.web;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import sharedkernel.domain.exceptions.FormatException;

public final class Values {

    private Values() {}

    public static String text(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (!(value instanceof String text)) {
            throw new FormatException("'" + field + "' must be a string.");
        }
        return text;
    }

    public static float[] floats(Map<String, Object> body, String field) {
        if (!(body.get(field) instanceof List<?> values) || values.isEmpty()) {
            throw new FormatException("'" + field + "' must be a non-empty array of numbers.");
        }

        var result = new float[values.size()];
        for (var index = 0; index < values.size(); index++) {
            if (!(values.get(index) instanceof Number number)) {
                throw new FormatException("'" + field + "' must be a non-empty array of numbers.");
            }
            var value = number.floatValue();
            if (!Float.isFinite(value)) {
                throw new FormatException("'" + field + "' must contain only finite numbers.");
            }
            result[index] = value;
        }
        return result;
    }

    public static Instant instant(Map<String, Object> body, String field) {
        var value = text(body, field);
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new FormatException("'" + field + "' must be an ISO instant, like 2026-08-19T10:00:00Z.");
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
}
