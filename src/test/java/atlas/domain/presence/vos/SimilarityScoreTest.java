package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SimilarityScoreTest {

    @ParameterizedTest
    @ValueSource(doubles = {SimilarityScore.MIN_VALUE, 0.5, 0.92, SimilarityScore.MAX_VALUE})
    void shouldCreateScoreWhenValueIsInRange(double value) {
        var result = SimilarityScore.create(value);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, -1.0, 1.1, 2.0})
    void shouldFailWhenValueIsOutOfRange(double value) {
        var result = SimilarityScore.create(value);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.INVALID_SIMILARITY_SCORE);
    }

    @Test
    void shouldThrowWhenInternallyBuiltScoreIsOutOfRange() {
        assertThatThrownBy(() -> SimilarityScore.of(1.5)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldMeetThresholdWhenScoreIsAbove() {
        assertThat(SimilarityScore.of(0.9).meets(MatchThreshold.of(0.8))).isTrue();
    }

    @Test
    void shouldMeetThresholdWhenScoreEqualsThreshold() {
        assertThat(SimilarityScore.of(0.8).meets(MatchThreshold.of(0.8))).isTrue();
    }

    @Test
    void shouldNotMeetThresholdWhenScoreIsBelow() {
        assertThat(SimilarityScore.of(0.7999).meets(MatchThreshold.of(0.8))).isFalse();
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(SimilarityScore.of(0.92)).isEqualTo(SimilarityScore.of(0.92));
    }
}
