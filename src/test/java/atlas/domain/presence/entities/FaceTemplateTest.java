package atlas.domain.presence.entities;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FaceTemplateTest {

    private static final FaceTemplateId ID =
        FaceTemplateId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final Instant CAPTURED_AT = Instant.parse("2026-08-18T10:00:00Z");
    private static final ModelVersion MODEL = ModelVersion.of("arcface-r100-v1");

    @Test
    void shouldExposeItsDescriptorAndCaptureInstant() {
        var template = FaceTemplate.create(ID, descriptor(0.1f, 0.2f), CAPTURED_AT);

        assertThat(template.id()).isEqualTo(ID);
        assertThat(template.descriptor()).isEqualTo(descriptor(0.1f, 0.2f));
        assertThat(template.capturedAt()).isEqualTo(CAPTURED_AT);
    }

    @Test
    void shouldDelegateItsModelVersionToTheDescriptor() {
        var template = FaceTemplate.create(ID, descriptor(0.1f, 0.2f), CAPTURED_AT);

        assertThat(template.modelVersion()).isEqualTo(MODEL);
    }

    @Test
    void shouldBeEqualByIdentityNotByDescriptor() {
        var one = FaceTemplate.create(ID, descriptor(0.1f, 0.2f), CAPTURED_AT);
        var other = FaceTemplate.create(ID, descriptor(0.9f, 0.8f), CAPTURED_AT);

        assertThat(one).isEqualTo(other);
    }

    private static FaceDescriptor descriptor(float... values) {
        return FaceDescriptor.of(MODEL, values);
    }
}
