package atlas.domain.presence.events;

import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.entities.FaceTemplateId;
import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record FaceTemplateAddedEvent(BiometricProfileId profileId, FaceTemplateId templateId, Instant occurredOn)
    implements DomainEvent {}
