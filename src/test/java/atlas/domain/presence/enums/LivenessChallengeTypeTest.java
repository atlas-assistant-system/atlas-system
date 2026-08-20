package atlas.domain.presence.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LivenessChallengeTypeTest {

    @Test
    void shouldOnlyExposeTheFistChallenge() {
        assertThat(LivenessChallengeType.values()).containsExactly(LivenessChallengeType.FIST);
        assertThat(LivenessChallengeType.FIST.timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(LivenessChallengeType.valueOf("FIST")).isEqualTo(LivenessChallengeType.FIST);
    }
}
