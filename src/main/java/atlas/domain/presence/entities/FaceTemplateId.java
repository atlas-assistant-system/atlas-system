package atlas.domain.presence.entities;

import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.UUID;

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
