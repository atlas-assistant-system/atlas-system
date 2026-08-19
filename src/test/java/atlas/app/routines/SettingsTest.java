package atlas.app.routines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SettingsTest {

    private static final UnaryOperator<String> NOTHING = key -> null;

    @Test
    void shouldFallBackToTheDefaults() {
        var settings = RoutinesSettings.from(NOTHING, NOTHING);

        assertThat(settings.port()).isEqualTo(RoutinesSettings.DEFAULT_PORT);
        assertThat(settings.dataDirectory()).isEqualTo(Path.of(RoutinesSettings.DEFAULT_DATA_DIRECTORY));
    }

    @Test
    void shouldReadTheSystemProperties() {
        var settings = RoutinesSettings.from(
            of(RoutinesSettings.PORT_PROPERTY, "9090", RoutinesSettings.DATA_DIRECTORY_PROPERTY, "/tmp/routines"),
            NOTHING);

        assertThat(settings.port()).isEqualTo(9090);
        assertThat(settings.dataDirectory()).isEqualTo(Path.of("/tmp/routines"));
    }

    @Test
    void shouldReadTheEnvironmentWhenNoPropertyIsSet() {
        var settings = RoutinesSettings.from(NOTHING, of(RoutinesSettings.PORT_VARIABLE, "7000"));

        assertThat(settings.port()).isEqualTo(7000);
    }

    @Test
    void shouldPreferThePropertyOverTheEnvironment() {
        var settings = RoutinesSettings.from(
            of(RoutinesSettings.PORT_PROPERTY, "9090"), of(RoutinesSettings.PORT_VARIABLE, "7000"));

        assertThat(settings.port()).isEqualTo(9090);
    }

    @Test
    void shouldIgnoreBlankValues() {
        var settings = RoutinesSettings.from(of(RoutinesSettings.PORT_PROPERTY, "   "), NOTHING);

        assertThat(settings.port()).isEqualTo(RoutinesSettings.DEFAULT_PORT);
    }

    @Test
    void shouldTrimTheValue() {
        assertThat(RoutinesSettings.from(of(RoutinesSettings.PORT_PROPERTY, " 9090 "), NOTHING).port()).isEqualTo(9090);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ocho mil", "-1", "65536"})
    void shouldRejectAnInvalidPort(String port) {
        assertThatThrownBy(() -> RoutinesSettings.from(of(RoutinesSettings.PORT_PROPERTY, port), NOTHING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(port);
    }

    @Test
    void shouldAcceptPortZeroToLetTheSystemChoose() {
        assertThat(RoutinesSettings.from(of(RoutinesSettings.PORT_PROPERTY, "0"), NOTHING).port()).isZero();
    }

    private static UnaryOperator<String> of(String... pairs) {
        var values = new java.util.HashMap<String, String>();

        for (var index = 0; index < pairs.length; index += 2) {
            values.put(pairs[index], pairs[index + 1]);
        }

        return Map.copyOf(values)::get;
    }
}
