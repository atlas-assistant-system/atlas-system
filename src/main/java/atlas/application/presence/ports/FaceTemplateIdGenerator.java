package atlas.application.presence.ports;

import atlas.domain.presence.entities.FaceTemplateId;

public interface FaceTemplateIdGenerator {

    FaceTemplateId next();
}
