package atlas.presentation.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import sharedkernel.domain.exceptions.FormatException;

class JsonTest {

    @Test
    void shouldKeepTheOrderOfTheFields() {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", "R00000001");
        body.put("name", "Correr");

        assertThat(Json.write(body)).isEqualTo("{\"id\":\"R00000001\",\"name\":\"Correr\"}");
    }

    @Test
    void shouldParseAJsonObject() {
        assertThat(Json.parse("{\"name\":\"Correr\",\"target\":3}"))
            .containsEntry("name", "Correr")
            .containsEntry("target", 3);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectAnEmptyBody(String body) {
        assertThatThrownBy(() -> Json.parse(body)).isInstanceOf(FormatException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldTreatAnEmptyBodyAsEmptyWhenOptional(String body) {
        assertThat(Json.parseOptional(body)).isEqualTo(Map.of());
    }

    @Test
    void shouldRejectMalformedJson() {
        assertThatThrownBy(() -> Json.parse("{no soy json}")).isInstanceOf(FormatException.class);
    }
}
