package atlas.domain.sharedkernel.results;

public record Error(String code, String message, ErrorType type) {

    public static final Error NONE = new Error("", "", ErrorType.NONE);

    public static Error validation(String code, String message) {
        return new Error(code, message, ErrorType.VALIDATION);
    }

    public static Error notFound(String code, String message) {
        return new Error(code, message, ErrorType.NOT_FOUND);
    }

    public static Error conflict(String code, String message) {
        return new Error(code, message, ErrorType.CONFLICT);
    }

    public static Error unauthorized(String code, String message) {
        return new Error(code, message, ErrorType.UNAUTHORIZED);
    }

    public static Error forbidden(String code, String message) {
        return new Error(code, message, ErrorType.FORBIDDEN);
    }

    public static Error failure(String code, String message) {
        return new Error(code, message, ErrorType.FAILURE);
    }

    public static Error unexpected(String code, String message) {
        return new Error(code, message, ErrorType.UNEXPECTED);
    }

    @Override
    public String toString() {
        return "[" + code + "] " + message;
    }
}
