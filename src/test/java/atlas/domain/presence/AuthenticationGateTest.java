package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.enums.VerificationOutcome;
import atlas.domain.presence.events.AuthenticationFailedEvent;
import atlas.domain.presence.events.AuthenticationSucceededEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import sharedkernel.domain.exceptions.GuardException;

class AuthenticationGateTest {

    private static final Instant NOW = Instant.parse("2026-08-18T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-08-18T10:05:00Z");
    private static final BiometricProfileId PROFILE_ID = BiometricProfileId.of(7);
    private static final SessionId SESSION_ID = SessionId.of(42);

    @Test
    void shouldStartWithoutFailuresAndWithoutEvents() {
        var gate = AuthenticationGate.initial();

        assertThat(gate.id()).isEqualTo(AuthenticationGateId.single());
        assertThat(gate.failedAttempts()).isZero();
        assertThat(gate.pendingEvents()).isEmpty();
    }

    @Test
    void shouldRestoreTheFailureCounterWithoutRaisingEvents() {
        var gate = AuthenticationGate.rehydrate(4);

        assertThat(gate.failedAttempts()).isEqualTo(4);
        assertThat(gate.pendingEvents()).isEmpty();
    }

    @Test
    void shouldRejectANegativeFailureCounter() {
        assertThatThrownBy(() -> AuthenticationGate.rehydrate(-1)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldKeepAcceptingFailuresWithoutLocking() {
        var gate = AuthenticationGate.initial();

        for (var i = 0; i < 20; i++) {
            gate.registerFailure(VerificationOutcome.NO_MATCH, NOW.plusSeconds(i));
        }

        assertThat(gate.failedAttempts()).isEqualTo(20);
        assertThat(gate.pendingEvents()).hasSize(20).allMatch(AuthenticationFailedEvent.class::isInstance);
    }

    @Test
    void shouldAuditEachFailure() {
        var gate = AuthenticationGate.initial();

        gate.registerFailure(VerificationOutcome.LIVENESS_FAILED, NOW);

        assertThat(gate.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(AuthenticationFailedEvent.class, event -> {
                assertThat(event.outcome()).isEqualTo(VerificationOutcome.LIVENESS_FAILED);
                assertThat(event.occurredOn()).isEqualTo(NOW);
            });
    }

    @ParameterizedTest
    @EnumSource(value = VerificationOutcome.class, names = {"MATCHED", "NO_PROFILES_ENROLLED"})
    void shouldRejectOutcomesThatAreNotFailedVerifications(VerificationOutcome outcome) {
        var gate = AuthenticationGate.initial();

        assertThatThrownBy(() -> gate.registerFailure(outcome, NOW)).isInstanceOf(GuardException.class);
        assertThat(gate.failedAttempts()).isZero();
    }

    @Test
    void shouldResetTheCounterOnSuccess() {
        var gate = AuthenticationGate.rehydrate(20);

        gate.registerSuccess(PROFILE_ID, SESSION_ID, LATER);

        assertThat(gate.failedAttempts()).isZero();
        assertThat(gate.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(AuthenticationSucceededEvent.class, event -> {
                assertThat(event.profileId()).isEqualTo(PROFILE_ID);
                assertThat(event.sessionId()).isEqualTo(SESSION_ID);
                assertThat(event.occurredOn()).isEqualTo(LATER);
            });
    }
}
