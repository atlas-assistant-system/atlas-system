package atlas.domain.presence.vos;

import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.time.Instant;

public record LivenessEvidence(ChallengeNonce nonce, LivenessChallengeType observedType, Instant capturedAt)
    implements ValueObject {

    public LivenessEvidence {
        ObjectGuard.notNull(nonce, "nonce");
        ObjectGuard.notNull(observedType, "observedType");
        ObjectGuard.notNull(capturedAt, "capturedAt");
    }

    public static LivenessEvidence of(ChallengeNonce nonce, LivenessChallengeType observedType, Instant capturedAt) {
        return new LivenessEvidence(nonce, observedType, capturedAt);
    }
}
