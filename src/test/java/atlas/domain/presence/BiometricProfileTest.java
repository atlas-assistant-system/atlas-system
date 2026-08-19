package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.entities.FaceTemplate;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.presence.events.FaceTemplateAddedEvent;
import atlas.domain.presence.events.FaceTemplateRemovedEvent;
import atlas.domain.presence.events.ProfileEnrolledEvent;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.ProfileName;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class BiometricProfileTest {

    private static final BiometricProfileId ID = BiometricProfileId.of(1);
    private static final ProfileName NAME = ProfileName.of("Eduardo");
    private static final Instant NOW = Instant.parse("2026-08-18T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-08-18T10:30:00Z");
    private static final ModelVersion MODEL = ModelVersion.of("arcface-r100-v1");
    private static final ModelVersion ANOTHER_MODEL = ModelVersion.of("arcface-r100-v2");

    @Test
    void shouldEnrollWithItsFirstTemplate() {
        var profile = BiometricProfile.enroll(ID, NAME, templateId(1), descriptor(0.1f, 0.2f), NOW);

        assertThat(profile.displayName()).isEqualTo(NAME);
        assertThat(profile.templates())
            .singleElement()
            .satisfies(template -> {
                assertThat(template.id()).isEqualTo(templateId(1));
                assertThat(template.descriptor()).isEqualTo(descriptor(0.1f, 0.2f));
                assertThat(template.capturedAt()).isEqualTo(NOW);
            });
    }

    @Test
    void shouldRaiseEnrolledEventWhenProfileIsCreated() {
        var profile = BiometricProfile.enroll(ID, NAME, templateId(1), descriptor(0.1f, 0.2f), NOW);

        assertThat(profile.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(ProfileEnrolledEvent.class, event -> {
                assertThat(event.profileId()).isEqualTo(ID);
                assertThat(event.displayName()).isEqualTo(NAME);
                assertThat(event.occurredOn()).isEqualTo(NOW);
            });
    }

    @Test
    void shouldExposeTemplatesAsUnmodifiable() {
        assertThat(enrolled().templates()).isUnmodifiable();
    }

    @Test
    void shouldExposeTheModelVersionSharedByItsTemplates() {
        assertThat(enrolled().modelVersion()).isEqualTo(MODEL);
    }

    @Test
    void shouldAddTemplateWhenModelVersionAndDimensionMatch() {
        var profile = enrolled();

        var result = profile.addTemplate(templateId(2), descriptor(0.3f, 0.4f), LATER);

        assertThat(result.isSuccess()).isTrue();
        assertThat(profile.templates()).hasSize(2);
        assertThat(profile.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(FaceTemplateAddedEvent.class, event -> {
                assertThat(event.profileId()).isEqualTo(ID);
                assertThat(event.templateId()).isEqualTo(templateId(2));
                assertThat(event.occurredOn()).isEqualTo(LATER);
            });
    }

    @Test
    void shouldRecordWhenTheAddedTemplateWasCaptured() {
        var profile = enrolled();

        profile.addTemplate(templateId(2), descriptor(0.3f, 0.4f), LATER);

        assertThat(profile.templates().getLast().capturedAt()).isEqualTo(LATER);
    }

    @Test
    void shouldAcceptTemplatesUpToTheMaximum() {
        var profile = enrolled();

        for (var i = 2; i <= BiometricProfile.MAX_TEMPLATES; i++) {
            profile.addTemplate(templateId(i), descriptor(0.3f, 0.4f), NOW);
        }

        assertThat(profile.templates()).hasSize(BiometricProfile.MAX_TEMPLATES);
    }

    @Test
    void shouldRejectTemplateBeyondTheMaximum() {
        var profile = enrolled();
        for (var i = 2; i <= BiometricProfile.MAX_TEMPLATES; i++) {
            profile.addTemplate(templateId(i), descriptor(0.3f, 0.4f), NOW);
        }

        var result = profile.addTemplate(templateId(99), descriptor(0.5f, 0.6f), NOW);

        assertThat(result.error()).isEqualTo(PresenceErrors.TOO_MANY_TEMPLATES);
        assertThat(profile.templates()).hasSize(BiometricProfile.MAX_TEMPLATES);
    }

    @Test
    void shouldRejectTemplateFromAnotherModelVersion() {
        var profile = enrolled();
        var foreign = FaceDescriptor.of(ANOTHER_MODEL, new float[]{0.3f, 0.4f});

        var result = profile.addTemplate(templateId(2), foreign, NOW);

        assertThat(result.error()).isEqualTo(PresenceErrors.MODEL_VERSION_MISMATCH);
        assertThat(profile.templates()).hasSize(1);
    }

    @Test
    void shouldRejectTemplateWithADifferentDimension() {
        var profile = enrolled();

        var result = profile.addTemplate(templateId(2), descriptor(0.3f, 0.4f, 0.5f), NOW);

        assertThat(result.error()).isEqualTo(PresenceErrors.DESCRIPTOR_DIMENSION_MISMATCH);
        assertThat(profile.templates()).hasSize(1);
    }

    @Test
    void shouldRemoveATemplateWhenOthersRemain() {
        var profile = withTwoTemplates();

        var result = profile.removeTemplate(templateId(1), LATER);

        assertThat(result.isSuccess()).isTrue();
        assertThat(profile.templates())
            .singleElement()
            .satisfies(template -> assertThat(template.id()).isEqualTo(templateId(2)));
        assertThat(profile.pendingEvents())
            .singleElement()
            .isInstanceOfSatisfying(FaceTemplateRemovedEvent.class, event -> {
                assertThat(event.profileId()).isEqualTo(ID);
                assertThat(event.templateId()).isEqualTo(templateId(1));
                assertThat(event.occurredOn()).isEqualTo(LATER);
            });
    }

    @Test
    void shouldFailWhenRemovingATemplateThatIsNotThere() {
        var profile = enrolled();

        var result = profile.removeTemplate(templateId(99), NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("Profile.TemplateNotFound");
        assertThat(profile.templates()).hasSize(1);
    }

    @Test
    void shouldFailWhenRemovingTheLastTemplate() {
        var profile = enrolled();

        var result = profile.removeTemplate(templateId(1), NOW);

        assertThat(result.error()).isEqualTo(PresenceErrors.LAST_TEMPLATE_CANNOT_BE_REMOVED);
        assertThat(profile.templates()).hasSize(1);
    }

    @Test
    void shouldRestoreItsTemplatesWhenRehydrated() {
        var template = FaceTemplate.create(templateId(1), descriptor(0.1f, 0.2f), NOW);

        var profile = BiometricProfile.rehydrate(ID, NAME, List.of(template));

        assertThat(profile.templates()).containsExactly(template);
        assertThat(profile.displayName()).isEqualTo(NAME);
    }

    @Test
    void shouldNotRaiseAnyEventWhenRehydrated() {
        var template = FaceTemplate.create(templateId(1), descriptor(0.1f, 0.2f), NOW);

        assertThat(BiometricProfile.rehydrate(ID, NAME, List.of(template)).pendingEvents()).isEmpty();
    }

    @Test
    void shouldThrowWhenRehydratedWithoutTemplates() {
        assertThatThrownBy(() -> BiometricProfile.rehydrate(ID, NAME, List.of())).isInstanceOf(GuardException.class);
    }

    private static BiometricProfile enrolled() {
        var profile = BiometricProfile.enroll(ID, NAME, templateId(1), descriptor(0.1f, 0.2f), NOW);
        profile.clearEvents();

        return profile;
    }

    private static BiometricProfile withTwoTemplates() {
        var profile = enrolled();
        profile.addTemplate(templateId(2), descriptor(0.3f, 0.4f), NOW);
        profile.clearEvents();

        return profile;
    }

    private static FaceDescriptor descriptor(float... values) {
        return FaceDescriptor.of(MODEL, values);
    }

    private static FaceTemplateId templateId(int seed) {
        return FaceTemplateId.of(new UUID(0, seed));
    }
}
