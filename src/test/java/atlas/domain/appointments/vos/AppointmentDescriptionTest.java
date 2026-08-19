package atlas.domain.appointments.vos;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.appointments.AppointmentErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AppointmentDescriptionTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldReturnEmptyWhenTextIsAbsentOrBlank(String candidate) {
        var result = AppointmentDescription.create(candidate);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEmpty();
    }

    @Test
    void shouldReturnDescriptionWhenTextIsPresent() {
        var result = AppointmentDescription.create("  Llevar radiografia  ");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).map(AppointmentDescription::value).contains("Llevar radiografia");
    }

    @Test
    void shouldAcceptDescriptionAtMaximumLength() {
        var result = AppointmentDescription.create("a".repeat(AppointmentDescription.MAX_LENGTH));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isPresent();
    }

    @Test
    void shouldFailWhenDescriptionExceedsMaximumLength() {
        var result = AppointmentDescription.create("a".repeat(AppointmentDescription.MAX_LENGTH + 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.DESCRIPTION_TOO_LONG);
    }
}
