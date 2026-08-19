package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.PresenceErrors;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class SessionDurationTest {

    @Test
    void shouldCreateDurationWhenPositiveAndWithinLimit() {
        var result = SessionDuration.create(Duration.ofMinutes(30));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().value()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void shouldAcceptDurationAtMaximumLength() {
        var result = SessionDuration.create(SessionDuration.MAX_DURATION);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().value()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void shouldFailWhenDurationIsMissing() {
        var result = SessionDuration.create(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.INVALID_SESSION_DURATION);
    }

    @Test
    void shouldFailWhenDurationIsZero() {
        var result = SessionDuration.create(Duration.ZERO);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.INVALID_SESSION_DURATION);
    }

    @Test
    void shouldFailWhenDurationIsNegative() {
        var result = SessionDuration.create(Duration.ofSeconds(-1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.INVALID_SESSION_DURATION);
    }

    @Test
    void shouldFailWhenDurationExceedsTwentyFourHours() {
        var result = SessionDuration.create(SessionDuration.MAX_DURATION.plusNanos(1));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.INVALID_SESSION_DURATION);
    }

    @Test
    void shouldThrowWhenInternallyBuiltDurationIsInvalid() {
        assertThatThrownBy(() -> SessionDuration.of(Duration.ZERO)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldKeepItsValueWhenInternallyBuilt() {
        assertThat(SessionDuration.of(Duration.ofMinutes(30)).value()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(SessionDuration.of(Duration.ofMinutes(30))).isEqualTo(SessionDuration.of(Duration.ofMinutes(30)));
    }

    @Test
    void shouldNotBeEqualWhenValuesDiffer() {
        assertThat(SessionDuration.of(Duration.ofMinutes(30)))
            .isNotEqualTo(SessionDuration.of(Duration.ofMinutes(60)));
    }
}
