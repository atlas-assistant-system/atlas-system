package atlas.presentation.common.web;

import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.util.Map;

public final class Json {

    private Json() {}

    public static String write(Object value) {
        try {
            return JSON.std.asString(value);
        } catch (IOException e) {
            throw new PersistenceException("Failed to serialize a response body", e);
        }
    }

    public static Map<String, Object> parseOptional(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }

        return parse(body);
    }

    public static Map<String, Object> parse(String body) {
        if (body == null || body.isBlank()) {
            throw new FormatException("The request body must be a JSON object.");
        }

        try {
            return JSON.std.mapFrom(body);
        } catch (IOException e) {
            throw new FormatException("The request body is not valid JSON.");
        }
    }
}
