package atlas.domain.presence.vos;

import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;

public record Match(FaceTemplateId templateId, SimilarityScore score) implements ValueObject {

    public Match {
        ObjectGuard.notNull(templateId, "templateId");
        ObjectGuard.notNull(score, "score");
    }

    public static Match of(FaceTemplateId templateId, SimilarityScore score) {
        return new Match(templateId, score);
    }
}
