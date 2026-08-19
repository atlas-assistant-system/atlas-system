package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.presence.events.LivenessChallengeIssuedEvent;
import atlas.domain.presence.vos.ChallengeNonce;
import atlas.domain.presence.vos.LivenessEvidence;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LivenessChallengeTest {

    private static final LivenessChallengeId ID = LivenessChallengeId.of(1);
    private static final Instant ISSUED_AT = Instant.parse("2026-08-17T10:00:00Z");
    private static final Instant STILL_ALIVE = Instant.parse("2026-08-17T10:00:04Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-08-17T10:00:10Z");
    private static final Instant AFTER_EXPIRY = Instant.parse("2026-08-17T10:00:11Z");
    private static final Instant BEFORE_ISSUE = Instant.parse("2026-08-17T09:59:59Z");

    private static final ChallengeNonce NONCE = ChallengeNonce.of("0123456789abcdef0123456789abcdef");
    private static final ChallengeNonce WRONG_NONCE = ChallengeNonce.of("ffffffffffffffffffffffffffffffff");

    @Test
    void shouldIssueChallengeThatExpiresAfterItsTypeTimeout() {
        var challenge = LivenessChallenge.issue(ID, LivenessChallengeType.VICTORY, NONCE, ISSUED_AT);

        assertThat(challenge.id()).isEqualTo(ID);
        assertThat(challenge.type()).isEqualTo(LivenessChallengeType.VICTORY);
        assertThat(challenge.nonce()).isEqualTo(NONCE);
        assertThat(challenge.issuedAt()).isEqualTo(ISSUED_AT);
        assertThat(challenge.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void shouldStartUnconsumedWhenIssued() {
        var challenge = LivenessChallenge.issue(ID, LivenessChallengeType.VICTORY, NONCE, ISSUED_AT);

        assertThat(challenge.isConsumed()).isFalse();
        assertThat(challenge.consumedAt()).isEmpty();
    }

    @Test
    void shouldRaiseIssuedEventWhenChallengeIsIssued() {
        var challenge = LivenessChallenge.issue(ID, LivenessChallengeType.VICTORY, NONCE, ISSUED_AT);

        assertThat(challenge.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(LivenessChallengeIssuedEvent.class, event -> {
                assertThat(event.challengeId()).isEqualTo(ID);
                assertThat(event.type()).isEqualTo(LivenessChallengeType.VICTORY);
                assertThat(event.occurredOn()).isEqualTo(ISSUED_AT);
            });
    }

    @Test
    void shouldNotCarryTheNonceInTheIssuedEvent() {
        var challenge = LivenessChallenge.issue(ID, LivenessChallengeType.VICTORY, NONCE, ISSUED_AT);

        assertThat(challenge.pendingEvents().getFirst().toString()).doesNotContain(NONCE.value());
    }

    @Test
    void shouldNotExposeTheNonceInToString() {
        var challenge = LivenessChallenge.issue(ID, LivenessChallengeType.VICTORY, NONCE, ISSUED_AT);

        assertThat(challenge.toString()).doesNotContain(NONCE.value());
    }

    @Test
    void shouldConsumeChallengeWhenEvidenceIsValid() {
        var challenge = issued();

        var result = challenge.consume(validEvidence(), STILL_ALIVE);

        assertThat(result.isSuccess()).isTrue();
        assertThat(challenge.isConsumed()).isTrue();
        assertThat(challenge.consumedAt()).contains(STILL_ALIVE);
        assertThat(challenge.pendingEvents()).isEmpty();
    }

    @Test
    void shouldAcceptEvidenceCapturedExactlyAtIssue() {
        var challenge = issued();

        var result = challenge.consume(evidence(NONCE, LivenessChallengeType.VICTORY, ISSUED_AT), STILL_ALIVE);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldFailWhenConsumingTwiceEvenWithValidEvidence() {
        var challenge = issued();
        challenge.consume(validEvidence(), STILL_ALIVE);

        var result = challenge.consume(validEvidence(), STILL_ALIVE);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_ALREADY_USED);
    }

    @Test
    void shouldReportAlreadyUsedBeforeExpiredWhenAConsumedChallengeExpires() {
        var challenge = issued();
        challenge.consume(validEvidence(), STILL_ALIVE);

        var result = challenge.consume(validEvidence(), AFTER_EXPIRY);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_ALREADY_USED);
    }

    @Test
    void shouldFailWhenChallengeHasExpired() {
        var challenge = issued();

        var result = challenge.consume(validEvidence(), AFTER_EXPIRY);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED);
        assertThat(challenge.isConsumed()).isFalse();
    }

    @Test
    void shouldFailWhenConsumingExactlyAtExpiry() {
        var challenge = issued();

        var result = challenge.consume(validEvidence(), EXPIRES_AT);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED);
    }

    @Test
    void shouldBeExpiredExactlyAtExpiry() {
        assertThat(issued().isExpired(EXPIRES_AT)).isTrue();
    }

    @Test
    void shouldNotBeExpiredJustBeforeExpiry() {
        assertThat(issued().isExpired(STILL_ALIVE)).isFalse();
    }

    @Test
    void shouldFailWhenEvidenceWasCapturedBeforeIssue() {
        var challenge = issued();

        var result = challenge.consume(evidence(NONCE, LivenessChallengeType.VICTORY, BEFORE_ISSUE), STILL_ALIVE);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED);
    }

    @Test
    void shouldFailWhenEvidenceWasCapturedAtExpiry() {
        var challenge = issued();

        var result = challenge.consume(evidence(NONCE, LivenessChallengeType.VICTORY, EXPIRES_AT), STILL_ALIVE);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED);
    }

    @Test
    void shouldReportExpiredBeforeLivenessFailureWhenEverythingIsWrong() {
        var challenge = issued();

        var result =
            challenge.consume(evidence(WRONG_NONCE, LivenessChallengeType.VICTORY, AFTER_EXPIRY), AFTER_EXPIRY);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED);
    }

    @Test
    void shouldFailWhenNonceDoesNotMatch() {
        var challenge = issued();

        var result = challenge.consume(evidence(WRONG_NONCE, LivenessChallengeType.VICTORY, STILL_ALIVE), STILL_ALIVE);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_FAILED);
        assertThat(challenge.isConsumed()).isFalse();
    }

    @Test
    void shouldRemainConsumableAfterAFailedAttempt() {
        var challenge = issued();
        challenge.consume(evidence(WRONG_NONCE, LivenessChallengeType.VICTORY, STILL_ALIVE), STILL_ALIVE);

        var result = challenge.consume(validEvidence(), STILL_ALIVE);

        assertThat(result.isSuccess()).isTrue();
        assertThat(challenge.consumedAt()).contains(STILL_ALIVE);
    }

    private static LivenessChallenge issued() {
        var challenge = LivenessChallenge.issue(ID, LivenessChallengeType.VICTORY, NONCE, ISSUED_AT);
        challenge.clearEvents();

        return challenge;
    }

    private static LivenessEvidence validEvidence() {
        return evidence(NONCE, LivenessChallengeType.VICTORY, STILL_ALIVE);
    }

    private static LivenessEvidence evidence(
        ChallengeNonce nonce, LivenessChallengeType observedType, Instant capturedAt) {
        return LivenessEvidence.of(nonce, observedType, capturedAt);
    }
}
