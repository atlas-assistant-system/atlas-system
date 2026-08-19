package atlas.app.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingsTest {

    @Test
    void shouldUseSafeLocalDefaults() {
        var settings = PresenceSettings.from(new String[0], key -> null, key -> null);

        assertThat(settings.port()).isEqualTo(8081);
        assertThat(settings.dataDirectory()).isEqualTo(Path.of("data"));
        assertThat(settings.maintenanceMode()).isFalse();
        assertThat(settings.matchThreshold().value()).isEqualTo(0.8);
        assertThat(settings.sessionDuration().value()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void shouldReadPropertiesBeforeEnvironmentAndRecognizeMaintenanceMode() {
        var properties = Map.of(
            PresenceSettings.PORT_PROPERTY, " 0 ",
            PresenceSettings.DATA_DIRECTORY_PROPERTY, "var/presence",
            PresenceSettings.MATCH_THRESHOLD_PROPERTY, "0.91",
            PresenceSettings.SESSION_MINUTES_PROPERTY, "30");
        var environment = Map.of(PresenceSettings.PORT_VARIABLE, "9000");

        var settings = PresenceSettings.from(
            new String[]{"--maintenance"}, properties::get, environment::get);

        assertThat(settings.port()).isZero();
        assertThat(settings.dataDirectory()).isEqualTo(Path.of("var/presence"));
        assertThat(settings.maintenanceMode()).isTrue();
        assertThat(settings.matchThreshold().value()).isEqualTo(0.91);
        assertThat(settings.sessionDuration().value()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        var properties = new HashMap<String, String>();
        properties.put(PresenceSettings.PORT_PROPERTY, "70000");
        assertThatThrownBy(() -> PresenceSettings.from(new String[0], properties::get, key -> null))
            .isInstanceOf(IllegalArgumentException.class);

        properties.clear();
        properties.put(PresenceSettings.MATCH_THRESHOLD_PROPERTY, "1.1");
        assertThatThrownBy(() -> PresenceSettings.from(new String[0], properties::get, key -> null))
            .isInstanceOf(IllegalArgumentException.class);

        properties.clear();
        properties.put(PresenceSettings.SESSION_MINUTES_PROPERTY, "0");
        assertThatThrownBy(() -> PresenceSettings.from(new String[0], properties::get, key -> null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
