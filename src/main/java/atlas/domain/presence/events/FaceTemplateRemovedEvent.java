package atlas.domain.presence.events;

import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record FaceTemplateRemovedEvent(BiometricProfileId profileId, FaceTemplateId templateId, Instant occurredOn)
    implements DomainEvent {}
