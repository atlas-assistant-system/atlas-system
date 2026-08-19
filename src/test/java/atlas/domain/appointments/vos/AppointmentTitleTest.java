package atlas.domain.appointments.vos;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.appointments.AppointmentErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AppointmentTitleTest {

    @Test
    void shouldCreateTitleWhenTextIsPresent() {
        var result = AppointmentTitle.create("Dentista");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().value()).isEqualTo("Dentista");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldFailWhenTitleIsBlank(String candidate) {
        var result = AppointmentTitle.create(candidate);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.TITLE_REQUIRED);
    }

    @Test
    void shouldTrimSurroundingWhitespace() {
        var result = AppointmentTitle.create("  Dentista  ");

        assertThat(result.value().value()).isEqualTo("Dentista");
    }

    @Test
    void shouldAcceptTitleAtMaximumLength() {
        var result = AppointmentTitle.create("a".repeat(AppointmentTitle.MAX_LENGTH));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldFailWhenTitleExceedsMaximumLength() {
        var result = AppointmentTitle.create("a".repeat(AppointmentTitle.MAX_LENGTH + 1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(AppointmentErrors.TITLE_TOO_LONG);
    }

    @Test
    void shouldNotCountTrimmedWhitespaceTowardsTheLimit() {
        var result = AppointmentTitle.create("  " + "a".repeat(AppointmentTitle.MAX_LENGTH) + "  ");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(AppointmentTitle.create("Dentista").value())
            .isEqualTo(AppointmentTitle.create("Dentista").value());
    }
}
