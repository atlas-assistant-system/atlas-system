package atlas.app.appointments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class SettingsTest {

    private static final UnaryOperator<String> NOTHING = key -> null;

    @Test
    void shouldFallBackToTheDefaultsWhenNothingIsConfigured() {
        var settings = AppointmentsSettings.from(NOTHING, NOTHING);

        assertThat(settings.port()).isEqualTo(AppointmentsSettings.DEFAULT_PORT);
        assertThat(settings.dataDirectory()).isEqualTo(Path.of(AppointmentsSettings.DEFAULT_DATA_DIRECTORY));
        assertThat(settings.presenceUrl()).isEqualTo(URI.create(AppointmentsSettings.DEFAULT_PRESENCE_URL));
    }

    @Test
    void shouldReadPortAndDirectoryFromEnvironmentVariables() {
        var settings = AppointmentsSettings.from(NOTHING, environment(Map.of(
            AppointmentsSettings.PORT_VARIABLE, "9090",
            AppointmentsSettings.DATA_DIRECTORY_VARIABLE, "/srv/agenda",
            AppointmentsSettings.PRESENCE_URL_VARIABLE, "http://127.0.0.1:9091")));

        assertThat(settings.port()).isEqualTo(9090);
        assertThat(settings.dataDirectory()).isEqualTo(Path.of("/srv/agenda"));
        assertThat(settings.presenceUrl()).isEqualTo(URI.create("http://127.0.0.1:9091"));
    }

    @Test
    void shouldPreferSystemPropertiesOverEnvironmentVariables() {
        var settings = AppointmentsSettings.from(
            environment(Map.of(AppointmentsSettings.PORT_PROPERTY, "1234")),
            environment(Map.of(AppointmentsSettings.PORT_VARIABLE, "9090")));

        assertThat(settings.port()).isEqualTo(1234);
    }

    @Test
    void shouldIgnoreBlankValuesAndTrimTheRest() {
        var settings = AppointmentsSettings.from(NOTHING, environment(Map.of(
            AppointmentsSettings.PORT_VARIABLE, "   ", AppointmentsSettings.DATA_DIRECTORY_VARIABLE, "  citas  ")));

        assertThat(settings.port()).isEqualTo(AppointmentsSettings.DEFAULT_PORT);
        assertThat(settings.dataDirectory()).isEqualTo(Path.of("citas"));
    }

    @Test
    void shouldAcceptZeroToLetTheSystemPickAFreePort() {
        assertThat(
            AppointmentsSettings.from(NOTHING, environment(Map.of(AppointmentsSettings.PORT_VARIABLE, "0"))).port())
            .isZero();
    }

    @Test
    void shouldRejectAPortThatIsNotANumber() {
        assertThatThrownBy(() -> AppointmentsSettings.from(NOTHING,
            environment(Map.of(AppointmentsSettings.PORT_VARIABLE, "ocho mil"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ocho mil")
            .hasMessageContaining(AppointmentsSettings.PORT_VARIABLE);
    }

    @Test
    void shouldRejectAPortOutsideTheValidRange() {
        assertThatThrownBy(
            () -> AppointmentsSettings.from(NOTHING, environment(Map.of(AppointmentsSettings.PORT_VARIABLE, "70000"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("65535");

        assertThatThrownBy(
            () -> AppointmentsSettings.from(NOTHING, environment(Map.of(AppointmentsSettings.PORT_VARIABLE, "-1"))))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectAnInvalidPresenceUrl() {
        assertThatThrownBy(() -> AppointmentsSettings.from(
            NOTHING, environment(Map.of(AppointmentsSettings.PRESENCE_URL_VARIABLE, "not-a-url"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Presence URL");
    }

    private static UnaryOperator<String> environment(Map<String, String> values) {
        return values::get;
    }
}
