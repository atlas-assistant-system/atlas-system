package sharedkernel.presentation.errors;

import sharedkernel.domain.results.ErrorType;

public final class HttpStatuses {

    private HttpStatuses() {}

    public static int forErrorType(ErrorType type) {
        return switch (type) {
            case VALIDATION -> 400;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case UNAUTHORIZED -> 401;
            case FORBIDDEN -> 403;
            case FAILURE -> 400;
            case UNEXPECTED -> 500;
            case NONE -> 200;
        };
    }
}
