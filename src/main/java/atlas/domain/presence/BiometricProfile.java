package atlas.domain.presence;

import atlas.domain.presence.entities.FaceTemplate;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.presence.events.FaceTemplateAddedEvent;
import atlas.domain.presence.events.FaceTemplateRemovedEvent;
import atlas.domain.presence.events.ProfileEnrolledEvent;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.ProfileName;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.CollectionGuard;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class BiometricProfile extends AggregateRoot<BiometricProfileId> {

    public static final int MAX_TEMPLATES = 10;

    private final ProfileName displayName;
    private final List<FaceTemplate> templates = new ArrayList<>();

    private BiometricProfile(BiometricProfileId id, ProfileName displayName) {
        super(id);
        this.displayName = ObjectGuard.notNull(displayName, "displayName");
    }

    public static BiometricProfile enroll(
        BiometricProfileId id,
        ProfileName displayName,
        FaceTemplateId templateId,
        FaceDescriptor descriptor,
        Instant now) {

        var profile = new BiometricProfile(id, displayName);
        profile.templates.add(FaceTemplate.create(templateId, descriptor, now));
        profile.registerEvent(new ProfileEnrolledEvent(id, displayName, now));

        return profile;
    }

    public static BiometricProfile rehydrate(
        BiometricProfileId id, ProfileName displayName, List<FaceTemplate> templates) {

        CollectionGuard.notEmpty(templates, "templates");

        var profile = new BiometricProfile(id, displayName);
        profile.templates.addAll(templates);

        return profile;
    }

    public Result<Void> addTemplate(FaceTemplateId templateId, FaceDescriptor descriptor, Instant now) {
        if (templates.size() >= MAX_TEMPLATES) {
            return Result.failure(PresenceErrors.TOO_MANY_TEMPLATES);
        }

        if (!descriptor.modelVersion().equals(modelVersion())) {
            return Result.failure(PresenceErrors.MODEL_VERSION_MISMATCH);
        }

        if (descriptor.dimension() != enrolledDimension()) {
            return Result.failure(PresenceErrors.DESCRIPTOR_DIMENSION_MISMATCH);
        }

        templates.add(FaceTemplate.create(templateId, descriptor, now));
        registerEvent(new FaceTemplateAddedEvent(id(), templateId, now));

        return Result.success();
    }

    public Result<Void> removeTemplate(FaceTemplateId templateId, Instant now) {
        var template = findTemplate(templateId);
        if (template.isEmpty()) {
            return Result.failure(PresenceErrors.templateNotFound(templateId));
        }

        if (templates.size() == 1) {
            return Result.failure(PresenceErrors.LAST_TEMPLATE_CANNOT_BE_REMOVED);
        }

        templates.remove(template.get());
        registerEvent(new FaceTemplateRemovedEvent(id(), templateId, now));

        return Result.success();
    }

    public ProfileName displayName() {
        return displayName;
    }

    public List<FaceTemplate> templates() {
        return Collections.unmodifiableList(templates);
    }

    public ModelVersion modelVersion() {
        return templates.getFirst().modelVersion();
    }

    private int enrolledDimension() {
        return templates.getFirst().descriptor().dimension();
    }

    private Optional<FaceTemplate> findTemplate(FaceTemplateId templateId) {
        return templates.stream().filter(template -> template.id().equals(templateId)).findFirst();
    }
}
