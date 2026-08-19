package atlas.domain.presence.services;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.presence.events.LivenessChallengeIssuedEvent;
import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LivenessPolicyTest {

    private static final LivenessChallengeId ID = LivenessChallengeId.of(1);
    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");

    private final LivenessPolicy policy = new LivenessPolicy();

    @Test
    void shouldIssueTheSameChallengeWhenSeedIsTheSame() {
        var first = policy.issue(ID, new Random(42), NOW);
        var second = policy.issue(ID, new Random(42), NOW);

        assertThat(second.type()).isEqualTo(first.type());
        assertThat(second.nonce()).isEqualTo(first.nonce());
    }

    @Test
    void shouldIssueDifferentNoncesWhenSeedsDiffer() {
        var first = policy.issue(ID, new Random(1), NOW);
        var second = policy.issue(ID, new Random(2), NOW);

        assertThat(second.nonce()).isNotEqualTo(first.nonce());
    }

    @Test
    void shouldAlwaysIssueTheVictoryChallenge() {
        assertThat(policy.issue(ID, new Random(42), NOW).type()).isEqualTo(LivenessChallengeType.VICTORY);
    }

    @Test
    void shouldNeverRepeatTheNonceOverConsecutiveDraws() {
        var random = new Random(42);

        var first = policy.issue(ID, random, NOW);
        var second = policy.issue(ID, random, NOW);

        assertThat(second.nonce()).isNotEqualTo(first.nonce());
    }

    @Test
    void shouldIssueChallengeThroughTheAggregateFactory() {
        var challenge = policy.issue(ID, new Random(42), NOW);

        assertThat(challenge.id()).isEqualTo(ID);
        assertThat(challenge.issuedAt()).isEqualTo(NOW);
        assertThat(challenge.expiresAt()).isEqualTo(NOW.plus(challenge.type().timeout()));
        assertThat(challenge.isConsumed()).isFalse();
        assertThat(challenge.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(LivenessChallengeIssuedEvent.class, event -> {
                assertThat(event.challengeId()).isEqualTo(ID);
                assertThat(event.type()).isEqualTo(challenge.type());
                assertThat(event.occurredOn()).isEqualTo(NOW);
            });
    }
}
