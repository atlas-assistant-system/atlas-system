package atlas.domain.presence.entities;

import java.util.UUID;
import sharedkernel.domain.ddd.SingleValueObject;
import sharedkernel.domain.guards.ObjectGuard;

public record FaceTemplateId(UUID value) implements SingleValueObject<UUID> {

    public FaceTemplateId {
        ObjectGuard.notNull(value, "value");
    }

    public static FaceTemplateId of(UUID value) {
        return new FaceTemplateId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
