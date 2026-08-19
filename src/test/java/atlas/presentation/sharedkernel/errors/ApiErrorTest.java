package atlas.presentation.sharedkernel.errors;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.logging.CorrelationContext;
import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.results.Error;
import atlas.domain.sharedkernel.results.ErrorType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ApiErrorTest {

    private static final String LEAK = "Connection to /var/db/agenda.sqlite refused as user admin";

    @ParameterizedTest
    @CsvSource({
        "VALIDATION, 400",
        "NOT_FOUND, 404",
        "CONFLICT, 409",
        "UNAUTHORIZED, 401",
        "FORBIDDEN, 403",
        "FAILURE, 400",
        "UNEXPECTED, 500",
        "NONE, 200"
    })
    void shouldMapEveryErrorTypeToItsStatus(ErrorType type, int expectedStatus) {
        assertThat(HttpStatuses.forErrorType(type)).isEqualTo(expectedStatus);
    }

    @Test
    void shouldKeepMessageWhenErrorIsBusinessFailure() {
        var apiError = ApiError.from(Error.conflict("APPOINTMENT_CANCELLED", "The appointment is cancelled."));

        assertThat(apiError.status()).isEqualTo(409);
        assertThat(apiError.code()).isEqualTo("APPOINTMENT_CANCELLED");
        assertThat(apiError.message()).isEqualTo("The appointment is cancelled.");
    }

    @Test
    void shouldReplaceMessageWhenErrorIsUnexpected() {
        var apiError = ApiError.from(Error.unexpected("DB_DOWN", LEAK));

        assertThat(apiError.status()).isEqualTo(500);
        assertThat(apiError.message()).isEqualTo(ExceptionTranslator.UNEXPECTED_MESSAGE);
        assertThat(apiError.message()).doesNotContain("sqlite", "admin", "/var/db");
    }

    @Test
    void shouldNotLeakInternalsWhenTranslatingAnyUnknownException() {
        var apiError = ApiError.from(new IllegalStateException(LEAK));

        assertThat(apiError.status()).isEqualTo(500);
        assertThat(apiError.message()).doesNotContain("sqlite", "admin", "/var/db");
        assertThat(apiError.code()).isEqualTo(ExceptionTranslator.UNEXPECTED);
    }

    @Test
    void shouldNotLeakParameterNameWhenGuardEscapes() {
        var apiError = ApiError.from(GuardException.forParameter("organizerPassword", "cannot be null"));

        assertThat(apiError.status()).isEqualTo(500);
        assertThat(apiError.message()).doesNotContain("organizerPassword");
    }

    @Test
    void shouldAnswerBadRequestWhenInputIsMalformed() {
        var apiError = ApiError.from(new FormatException("ID must be 9 characters long."));

        assertThat(apiError.status()).isEqualTo(400);
        assertThat(apiError.code()).isEqualTo(ExceptionTranslator.MALFORMED_INPUT);
        assertThat(apiError.message()).isEqualTo("ID must be 9 characters long.");
    }

    @Test
    void shouldCarryCorrelationIdWhenScopeIsBound() {
        var captured = new ApiError[1];

        CorrelationContext.runWith("a3f9c1", () -> captured[0] = ApiError.from(new IllegalStateException(LEAK)));

        assertThat(captured[0].correlationId()).isEqualTo("a3f9c1");
    }

    @Test
    void shouldOmitCorrelationIdWhenScopeIsNotBound() {
        assertThat(ApiError.from(Error.notFound("NOT_FOUND", "missing")).correlationId()).isNull();
    }

    @Test
    void shouldExposeFieldErrorsWhenValidationFails() {
        var apiError = ApiError.from(
            Error.validation("INVALID_APPOINTMENT", "Invalid appointment."),
            List.of(new FieldError("timeSlot", "End must be after start.")));

        assertThat(apiError.status()).isEqualTo(400);
        assertThat(apiError.fieldErrors()).containsExactly(new FieldError("timeSlot", "End must be after start."));
    }

    @Test
    void shouldNeverBeNullWhenFieldErrorsAreAbsent() {
        assertThat(ApiError.from(Error.notFound("NOT_FOUND", "missing")).fieldErrors()).isEmpty();
    }
}
