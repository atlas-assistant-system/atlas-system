package atlas.domain.presence.events;

import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.enums.LivenessChallengeType;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record LivenessChallengeIssuedEvent(
    LivenessChallengeId challengeId, LivenessChallengeType type, Instant occurredOn)
    implements DomainEvent {}
