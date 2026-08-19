package atlas.infrastructure.common;

import atlas.application.presence.ports.FaceTemplateIdGenerator;
import atlas.domain.presence.entities.FaceTemplateId;
import java.util.UUID;

public final class UuidFaceTemplateIdGenerator implements FaceTemplateIdGenerator {

    @Override
    public FaceTemplateId next() {
        return FaceTemplateId.of(UUID.randomUUID());
    }
}
