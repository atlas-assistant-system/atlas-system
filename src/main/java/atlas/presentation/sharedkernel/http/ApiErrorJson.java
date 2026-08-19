package atlas.presentation.sharedkernel.http;

import atlas.presentation.sharedkernel.errors.ApiError;

final class ApiErrorJson {

    private ApiErrorJson() {}

    static String write(ApiError error) {
        var json = new StringBuilder("{\"status\":").append(error.status());

        json.append(",\"code\":").append(quote(error.code()));
        json.append(",\"message\":").append(quote(error.message()));

        if (error.correlationId() != null) {
            json.append(",\"correlationId\":").append(quote(error.correlationId()));
        }

        if (!error.fieldErrors().isEmpty()) {
            json.append(",\"fieldErrors\":").append(writeFieldErrors(error));
        }

        return json.append('}').toString();
    }

    static String quote(String value) {
        if (value == null) {
            return "null";
        }

        var quoted = new StringBuilder("\"");

        for (var index = 0; index < value.length(); index++) {
            quoted.append(escaped(value.charAt(index)));
        }

        return quoted.append('"').toString();
    }

    private static String writeFieldErrors(ApiError error) {
        var array = new StringBuilder("[");

        for (var fieldError : error.fieldErrors()) {
            if (array.length() > 1) {
                array.append(',');
            }

            array.append("{\"field\":").append(quote(fieldError.field()));
            array.append(",\"message\":").append(quote(fieldError.message())).append('}');
        }

        return array.append(']').toString();
    }

    private static String escaped(char character) {
        return switch (character) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> character < 0x20 ? String.format("\\u%04x", (int) character) : String.valueOf(character);
        };
    }
}
