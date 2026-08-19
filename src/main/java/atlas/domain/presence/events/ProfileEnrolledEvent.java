package atlas.domain.presence.events;

import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.vos.ProfileName;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record ProfileEnrolledEvent(BiometricProfileId profileId, ProfileName displayName, Instant occurredOn)
    implements DomainEvent {}
