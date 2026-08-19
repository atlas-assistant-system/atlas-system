package atlas.domain.presence.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LivenessChallengeTypeTest {

    @Test
    void shouldOnlyExposeTheSecretVictoryChallenge() {
        assertThat(LivenessChallengeType.values()).containsExactly(LivenessChallengeType.VICTORY);
        assertThat(LivenessChallengeType.VICTORY.timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(LivenessChallengeType.valueOf("VICTORY")).isEqualTo(LivenessChallengeType.VICTORY);
    }
}
