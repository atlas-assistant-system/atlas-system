package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.PresenceErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sharedkernel.domain.exceptions.GuardException;

class MatchThresholdTest {

    @ParameterizedTest
    @ValueSource(doubles = {MatchThreshold.MIN_VALUE, 0.5, 0.8, MatchThreshold.MAX_VALUE})
    void shouldCreateThresholdWhenValueIsInRange(double value) {
        var result = MatchThreshold.create(value);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, -1.0, 1.1, 2.0})
    void shouldFailWhenValueIsOutOfRange(double value) {
        var result = MatchThreshold.create(value);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.INVALID_MATCH_THRESHOLD);
    }

    @Test
    void shouldThrowWhenInternallyBuiltThresholdIsOutOfRange() {
        assertThatThrownBy(() -> MatchThreshold.of(-0.5)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(MatchThreshold.of(0.8)).isEqualTo(MatchThreshold.of(0.8));
    }
}
