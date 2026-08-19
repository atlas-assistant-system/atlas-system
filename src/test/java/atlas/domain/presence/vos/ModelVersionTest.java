package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ModelVersionTest {

    @Test
    void shouldCreateVersionWhenTextIsPresent() {
        var result = ModelVersion.create("arcface-v1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().value()).isEqualTo("arcface-v1");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldFailWhenVersionIsBlank(String candidate) {
        var result = ModelVersion.create(candidate);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.MODEL_VERSION_REQUIRED);
    }

    @Test
    void shouldThrowWhenInternallyBuiltVersionIsBlank() {
        assertThatThrownBy(() -> ModelVersion.of("   ")).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldKeepItsValueWhenInternallyBuilt() {
        assertThat(ModelVersion.of("arcface-v1").value()).isEqualTo("arcface-v1");
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(ModelVersion.of("arcface-v1")).isEqualTo(ModelVersion.of("arcface-v1"));
    }

    @Test
    void shouldNotBeEqualWhenValuesDiffer() {
        assertThat(ModelVersion.of("arcface-v1")).isNotEqualTo(ModelVersion.of("arcface-v2"));
    }

    @Test
    void shouldPrintItsValue() {
        assertThat(ModelVersion.of("arcface-v1")).hasToString("arcface-v1");
    }
}
