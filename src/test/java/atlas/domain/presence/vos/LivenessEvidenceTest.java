package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.sharedkernel.exceptions.GuardException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LivenessEvidenceTest {

    private static final ChallengeNonce NONCE = ChallengeNonce.of("359d41baf78afe0de1bbe7ae28c0450c");
    private static final Instant CAPTURED_AT = Instant.parse("2026-08-18T09:30:00Z");

    @Test
    void shouldExposeItsComponents() {
        var evidence = LivenessEvidence.of(NONCE, LivenessChallengeType.FIST, CAPTURED_AT);

        assertThat(evidence.nonce()).isEqualTo(NONCE);
        assertThat(evidence.observedType()).isEqualTo(LivenessChallengeType.FIST);
        assertThat(evidence.capturedAt()).isEqualTo(CAPTURED_AT);
    }

    @Test
    void shouldThrowWhenNonceIsMissing() {
        assertThatThrownBy(() -> LivenessEvidence.of(null, LivenessChallengeType.FIST, CAPTURED_AT))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenObservedTypeIsMissing() {
        assertThatThrownBy(() -> LivenessEvidence.of(NONCE, null, CAPTURED_AT))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenCapturedAtIsMissing() {
        assertThatThrownBy(() -> LivenessEvidence.of(NONCE, LivenessChallengeType.FIST, null))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualByValue() {
        assertThat(LivenessEvidence.of(NONCE, LivenessChallengeType.FIST, CAPTURED_AT))
            .isEqualTo(LivenessEvidence.of(NONCE, LivenessChallengeType.FIST, CAPTURED_AT));
    }
}
