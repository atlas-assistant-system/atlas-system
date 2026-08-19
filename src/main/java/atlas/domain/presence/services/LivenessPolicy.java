package atlas.domain.presence.services;

import atlas.domain.presence.LivenessChallenge;
import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.presence.vos.ChallengeNonce;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.time.Instant;
import java.util.random.RandomGenerator;

public final class LivenessPolicy {

    public LivenessChallenge issue(LivenessChallengeId id, RandomGenerator random, Instant now) {
        ObjectGuard.notNull(random, "random");

        return LivenessChallenge.issue(
            id, LivenessChallengeType.VICTORY, ChallengeNonce.generate(random), now);
    }
}
