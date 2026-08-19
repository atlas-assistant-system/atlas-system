package atlas.domain.presence.entities;

import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.sharedkernel.ddd.Entity;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.time.Instant;

public final class FaceTemplate extends Entity<FaceTemplateId> {

    private final FaceDescriptor descriptor;
    private final Instant capturedAt;

    private FaceTemplate(FaceTemplateId id, FaceDescriptor descriptor, Instant capturedAt) {
        super(id);
        this.descriptor = ObjectGuard.notNull(descriptor, "descriptor");
        this.capturedAt = ObjectGuard.notNull(capturedAt, "capturedAt");
    }

    public static FaceTemplate create(FaceTemplateId id, FaceDescriptor descriptor, Instant capturedAt) {
        return new FaceTemplate(id, descriptor, capturedAt);
    }

    public FaceDescriptor descriptor() {
        return descriptor;
    }

    public ModelVersion modelVersion() {
        return descriptor.modelVersion();
    }

    public Instant capturedAt() {
        return capturedAt;
    }
}
