package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.enums.LivenessChallengeType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class LivenessEvidenceTest {

    private static final ChallengeNonce NONCE = ChallengeNonce.of("359d41baf78afe0de1bbe7ae28c0450c");
    private static final Instant CAPTURED_AT = Instant.parse("2026-08-18T09:30:00Z");

    @Test
    void shouldExposeItsComponents() {
        var evidence = LivenessEvidence.of(NONCE, LivenessChallengeType.VICTORY, CAPTURED_AT);

        assertThat(evidence.nonce()).isEqualTo(NONCE);
        assertThat(evidence.observedType()).isEqualTo(LivenessChallengeType.VICTORY);
        assertThat(evidence.capturedAt()).isEqualTo(CAPTURED_AT);
    }

    @Test
    void shouldThrowWhenNonceIsMissing() {
        assertThatThrownBy(() -> LivenessEvidence.of(null, LivenessChallengeType.VICTORY, CAPTURED_AT))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenObservedTypeIsMissing() {
        assertThatThrownBy(() -> LivenessEvidence.of(NONCE, null, CAPTURED_AT))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenCapturedAtIsMissing() {
        assertThatThrownBy(() -> LivenessEvidence.of(NONCE, LivenessChallengeType.VICTORY, null))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(LivenessEvidence.of(NONCE, LivenessChallengeType.VICTORY, CAPTURED_AT))
            .isEqualTo(LivenessEvidence.of(NONCE, LivenessChallengeType.VICTORY, CAPTURED_AT));
    }
}
