package atlas.infrastructure.presence.memory;

import atlas.application.presence.ports.LivenessChallengeRepository;
import atlas.domain.presence.LivenessChallenge;
import atlas.domain.presence.LivenessChallengeId;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import sharedkernel.application.unitofwork.AggregateChanges;
import sharedkernel.domain.guards.ObjectGuard;

public final class InMemoryLivenessChallengeRepository implements LivenessChallengeRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<LivenessChallengeId, LivenessChallenge> challenges = new ConcurrentHashMap<>();

    @Override
    public LivenessChallengeId nextId() {
        return LivenessChallengeId.of(sequence.incrementAndGet());
    }

    @Override
    public void save(LivenessChallenge challenge) {
        ObjectGuard.notNull(challenge, "challenge");
        if (challenges.putIfAbsent(challenge.id(), challenge) != null) {
            throw new IllegalStateException("Liveness challenge already exists: " + challenge.id());
        }
        AggregateChanges.track(challenge);
    }

    @Override
    public Optional<LivenessChallenge> get(LivenessChallengeId id) {
        ObjectGuard.notNull(id, "id");
        return Optional.ofNullable(challenges.get(id));
    }

    @Override
    public void remove(LivenessChallengeId id) {
        ObjectGuard.notNull(id, "id");
        challenges.remove(id);
    }

    @Override
    public int removeExpired(Instant now) {
        ObjectGuard.notNull(now, "now");

        var removed = 0;
        for (var entry : challenges.entrySet()) {
            if (entry.getValue().isExpired(now) && challenges.remove(entry.getKey(), entry.getValue())) {
                removed++;
            }
        }
        return removed;
    }
}
