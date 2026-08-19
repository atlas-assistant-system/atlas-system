package atlas.application.presence.ports;

import atlas.domain.presence.LivenessChallenge;
import atlas.domain.presence.LivenessChallengeId;
import java.time.Instant;
import java.util.Optional;

public interface LivenessChallengeRepository {

    LivenessChallengeId nextId();

    void save(LivenessChallenge challenge);

    Optional<LivenessChallenge> get(LivenessChallengeId id);

    void remove(LivenessChallengeId id);

    int removeExpired(Instant now);
}
