package atlas.domain.presence;

import atlas.domain.presence.enums.VerificationOutcome;
import atlas.domain.presence.events.AuthenticationFailedEvent;
import atlas.domain.presence.events.AuthenticationSucceededEvent;
import java.time.Instant;
import sharedkernel.domain.ddd.AggregateRoot;
import sharedkernel.domain.exceptions.GuardException;
import sharedkernel.domain.guards.NumberGuard;
import sharedkernel.domain.guards.ObjectGuard;

public final class AuthenticationGate extends AggregateRoot<AuthenticationGateId> {

    private int failedAttempts;

    private AuthenticationGate(AuthenticationGateId id, int failedAttempts) {
        super(id);
        this.failedAttempts = NumberGuard.notNegative(failedAttempts, "failedAttempts");
    }

    public static AuthenticationGate initial() {
        return new AuthenticationGate(AuthenticationGateId.single(), 0);
    }

    public static AuthenticationGate rehydrate(int failedAttempts) {
        return new AuthenticationGate(AuthenticationGateId.single(), failedAttempts);
    }

    public void registerFailure(VerificationOutcome outcome, Instant now) {
        ObjectGuard.notNull(outcome, "outcome");
        if (!outcome.isFailedVerification()) {
            throw GuardException.forParameter("outcome", "must be a failed verification");
        }

        failedAttempts++;
        registerEvent(new AuthenticationFailedEvent(outcome, now));
    }

    public void registerSuccess(BiometricProfileId profileId, SessionId sessionId, Instant now) {
        this.failedAttempts = 0;
        registerEvent(new AuthenticationSucceededEvent(profileId, sessionId, now));
    }

    public int failedAttempts() {
        return failedAttempts;
    }

}
