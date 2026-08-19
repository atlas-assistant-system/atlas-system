package atlas.domain.sharedkernel.results;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorTest {

    @Test
    void shouldCarryCategoryWhenCreatedThroughFactory() {
        assertThat(Error.validation("C", "m").type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(Error.notFound("C", "m").type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(Error.conflict("C", "m").type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(Error.unauthorized("C", "m").type()).isEqualTo(ErrorType.UNAUTHORIZED);
        assertThat(Error.forbidden("C", "m").type()).isEqualTo(ErrorType.FORBIDDEN);
        assertThat(Error.failure("C", "m").type()).isEqualTo(ErrorType.FAILURE);
        assertThat(Error.unexpected("C", "m").type()).isEqualTo(ErrorType.UNEXPECTED);
    }

    @Test
    void shouldFormatCodeAndMessageWhenConvertedToString() {
        var error = Error.validation("Appointment.TimeSlotRequired", "A time slot is required.");

        assertThat(error.toString()).isEqualTo("[Appointment.TimeSlotRequired] A time slot is required.");
    }

    @Test
    void shouldBeEqualWhenCodeMessageAndTypeMatch() {
        assertThat(Error.validation("C", "m")).isEqualTo(Error.validation("C", "m"));
        assertThat(Error.validation("C", "m")).isNotEqualTo(Error.conflict("C", "m"));
    }
}
