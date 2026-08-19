package atlas.domain.presence;

import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.presence.events.LivenessChallengeIssuedEvent;
import atlas.domain.presence.vos.ChallengeNonce;
import atlas.domain.presence.vos.LivenessEvidence;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.util.Optional;

public final class LivenessChallenge extends AggregateRoot<LivenessChallengeId> {

    private final LivenessChallengeType type;
    private final ChallengeNonce nonce;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant consumedAt;

    private LivenessChallenge(
        LivenessChallengeId id,
        LivenessChallengeType type,
        ChallengeNonce nonce,
        Instant issuedAt,
        Instant expiresAt) {
        super(id);
        this.type = ObjectGuard.notNull(type, "type");
        this.nonce = ObjectGuard.notNull(nonce, "nonce");
        this.issuedAt = ObjectGuard.notNull(issuedAt, "issuedAt");
        this.expiresAt = ObjectGuard.notNull(expiresAt, "expiresAt");
    }

    public static LivenessChallenge issue(
        LivenessChallengeId id, LivenessChallengeType type, ChallengeNonce nonce, Instant now) {

        ObjectGuard.notNull(type, "type");
        ObjectGuard.notNull(now, "now");

        var challenge = new LivenessChallenge(id, type, nonce, now, now.plus(type.timeout()));
        challenge.registerEvent(new LivenessChallengeIssuedEvent(id, type, now));

        return challenge;
    }

    public Result<Void> consume(LivenessEvidence evidence, Instant now) {
        ObjectGuard.notNull(evidence, "evidence");
        ObjectGuard.notNull(now, "now");

        if (isConsumed()) {
            return Result.failure(PresenceErrors.LIVENESS_CHALLENGE_ALREADY_USED);
        }

        if (isExpired(now)) {
            return Result.failure(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED);
        }

        if (capturedOutsideWindow(evidence.capturedAt())) {
            return Result.failure(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED);
        }

        if (!nonce.matches(evidence.nonce())) {
            return Result.failure(PresenceErrors.LIVENESS_FAILED);
        }

        if (evidence.observedType() != type) {
            return Result.failure(PresenceErrors.LIVENESS_FAILED);
        }

        this.consumedAt = now;

        return Result.success();
    }

    public boolean isExpired(Instant now) {
        ObjectGuard.notNull(now, "now");

        return !now.isBefore(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public LivenessChallengeType type() {
        return type;
    }

    public ChallengeNonce nonce() {
        return nonce;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<Instant> consumedAt() {
        return Optional.ofNullable(consumedAt);
    }

    private boolean capturedOutsideWindow(Instant capturedAt) {
        return capturedAt.isBefore(issuedAt) || !capturedAt.isBefore(expiresAt);
    }
}
