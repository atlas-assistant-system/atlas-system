package atlas.domain.presence.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VerificationOutcomeTest {

    @Test
    void shouldConsiderOnlyMatchedAsSucceeded() {
        assertThat(VerificationOutcome.MATCHED.succeeded()).isTrue();
        assertThat(VerificationOutcome.NO_MATCH.succeeded()).isFalse();
        assertThat(VerificationOutcome.LIVENESS_FAILED.succeeded()).isFalse();
        assertThat(VerificationOutcome.NO_PROFILES_ENROLLED.succeeded()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "MATCHED, false",
        "NO_MATCH, true",
        "LIVENESS_FAILED, true",
        "NO_PROFILES_ENROLLED, false",
    })
    void shouldIdentifyOnlyFailedVerifications(VerificationOutcome outcome, boolean failed) {
        assertThat(outcome.isFailedVerification()).isEqualTo(failed);
    }

    @Test
    void shouldRoundTripThroughItsName() {
        assertThat(VerificationOutcome.valueOf("MATCHED")).isEqualTo(VerificationOutcome.MATCHED);
        assertThat(VerificationOutcome.LIVENESS_FAILED.name()).isEqualTo("LIVENESS_FAILED");
    }
}
