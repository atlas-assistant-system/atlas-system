package atlas.presentation.sharedkernel.errors;

import atlas.application.sharedkernel.logging.CorrelationContext;
import atlas.domain.sharedkernel.results.Error;
import atlas.domain.sharedkernel.results.ErrorType;
import java.util.List;

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
