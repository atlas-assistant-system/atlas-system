package atlas.domain.presence.events;

import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.vos.ProfileName;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record ProfileEnrolledEvent(BiometricProfileId profileId, ProfileName displayName, Instant occurredOn)
    implements DomainEvent {}
