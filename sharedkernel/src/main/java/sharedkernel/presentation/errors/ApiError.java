package sharedkernel.presentation.errors;

import java.util.List;
import sharedkernel.application.logging.CorrelationContext;
import sharedkernel.domain.results.Error;
import sharedkernel.domain.results.ErrorType;

public record ApiError(int status, String code, String message, String correlationId, List<FieldError> fieldErrors) {

    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiError from(Error error) {
        return from(error, List.of());
    }

    public static ApiError from(Error error, List<FieldError> fieldErrors) {
        var safeMessage = error.type() == ErrorType.UNEXPECTED
            ? ExceptionTranslator.UNEXPECTED_MESSAGE
            : error.message();

        return new ApiError(
            HttpStatuses.forErrorType(error.type()),
            error.code(),
            safeMessage,
            CorrelationContext.current().orElse(null),
            fieldErrors);
    }

    public static ApiError from(Throwable thrown) {
        return from(ExceptionTranslator.translate(thrown));
    }
}
