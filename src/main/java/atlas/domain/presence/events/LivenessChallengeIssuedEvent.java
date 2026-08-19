package atlas.domain.presence.events;

import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record LivenessChallengeIssuedEvent(
    LivenessChallengeId challengeId, LivenessChallengeType type, Instant occurredOn)
    implements DomainEvent {}
