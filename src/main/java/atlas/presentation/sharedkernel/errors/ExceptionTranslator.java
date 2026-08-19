package atlas.presentation.sharedkernel.errors;

import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.sharedkernel.results.Error;

public final class ExceptionTranslator {

    public static final String MALFORMED_INPUT = "MALFORMED_INPUT";
    public static final String UNEXPECTED = "UNEXPECTED";
    public static final String UNEXPECTED_MESSAGE = "An unexpected error occurred.";

    private ExceptionTranslator() {}

    public static Error translate(Throwable thrown) {
        if (thrown instanceof FormatException) {
            return Error.validation(MALFORMED_INPUT, thrown.getMessage());
        }

        return Error.unexpected(UNEXPECTED, UNEXPECTED_MESSAGE);
    }
}
