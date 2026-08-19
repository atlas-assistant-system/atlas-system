package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import atlas.domain.presence.PresenceErrors;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class FaceDescriptorTest {

    private static final ModelVersion MODEL = ModelVersion.of("arcface-v1");
    private static final ModelVersion OTHER_MODEL = ModelVersion.of("arcface-v2");

    @Test
    void shouldCreateDescriptorWhenValuesArePresent() {
        var result = FaceDescriptor.create(MODEL, new float[]{0.25f, 0.5f, 0.125f});

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().modelVersion()).isEqualTo(MODEL);
        assertThat(result.value().values()).containsExactly(0.25f, 0.5f, 0.125f);
        assertThat(result.value().dimension()).isEqualTo(3);
    }

    @Test
    void shouldFailWhenModelVersionIsMissing() {
        var result = FaceDescriptor.create(null, new float[]{0.25f});

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.MODEL_VERSION_REQUIRED);
    }

    @Test
    void shouldFailWhenValuesAreMissing() {
        var result = FaceDescriptor.create(MODEL, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.DESCRIPTOR_REQUIRED);
    }

    @Test
    void shouldFailWhenValuesAreEmpty() {
        var result = FaceDescriptor.create(MODEL, new float[0]);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(PresenceErrors.DESCRIPTOR_REQUIRED);
    }

    @Test
    void shouldThrowWhenInternallyBuiltDescriptorIsEmpty() {
        assertThatThrownBy(() -> FaceDescriptor.of(MODEL, new float[0])).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldBeEqualWhenVectorsHaveSameContentInDifferentArrays() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});
        var same = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});

        assertThat(descriptor).isEqualTo(same);
        assertThat(descriptor).hasSameHashCodeAs(same);
    }

    @Test
    void shouldNotBeEqualWhenVectorsDiffer() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});
        var different = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.75f});

        assertThat(descriptor).isNotEqualTo(different);
    }

    @Test
    void shouldSpreadHashCodesAcrossDifferentVectors() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});
        var different = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.75f});

        assertThat(descriptor.hashCode()).isNotEqualTo(different.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenModelVersionsDiffer() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});
        var different = FaceDescriptor.of(OTHER_MODEL, new float[]{0.25f, 0.5f});

        assertThat(descriptor).isNotEqualTo(different);
    }

    @Test
    void shouldCopyValuesOnConstruction() {
        var input = new float[]{0.25f, 0.5f};
        var descriptor = FaceDescriptor.of(MODEL, input);

        input[0] = 0.99f;

        assertThat(descriptor.values()).containsExactly(0.25f, 0.5f);
    }

    @Test
    void shouldCopyValuesOnAccess() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});

        descriptor.values()[0] = 0.99f;

        assertThat(descriptor.values()).containsExactly(0.25f, 0.5f);
    }

    @Test
    void shouldRedactVectorInToString() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.125f, 0.25f});

        assertThat(descriptor.toString()).isEqualTo("FaceDescriptor[model=arcface-v1, dimension=2]");
        assertThat(descriptor.toString()).doesNotContain("0.125");
    }

    @Test
    void shouldBeComparableWhenModelAndDimensionMatch() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});
        var candidate = FaceDescriptor.of(MODEL, new float[]{0.75f, 0.1f});

        assertThat(descriptor.isComparableWith(candidate)).isTrue();
    }

    @Test
    void shouldNotBeComparableWhenModelDiffers() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});
        var candidate = FaceDescriptor.of(OTHER_MODEL, new float[]{0.25f, 0.5f});

        assertThat(descriptor.isComparableWith(candidate)).isFalse();
    }

    @Test
    void shouldNotBeComparableWhenDimensionDiffers() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f});
        var candidate = FaceDescriptor.of(MODEL, new float[]{0.25f, 0.5f, 0.75f});

        assertThat(descriptor.isComparableWith(candidate)).isFalse();
    }

    @Test
    void shouldScoreOneWhenVectorsAreIdentical() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 2f, 3f});
        var identical = FaceDescriptor.of(MODEL, new float[]{1f, 2f, 3f});

        assertThat(descriptor.cosineSimilarity(identical).value()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void shouldScoreZeroWhenVectorsAreOrthogonal() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 0f});
        var orthogonal = FaceDescriptor.of(MODEL, new float[]{0f, 1f});

        assertThat(descriptor.cosineSimilarity(orthogonal).value()).isEqualTo(0.0);
    }

    @Test
    void shouldClampToZeroWhenVectorsAreOpposite() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 0f});
        var opposite = FaceDescriptor.of(MODEL, new float[]{-1f, 0f});

        assertThat(descriptor.cosineSimilarity(opposite).value()).isEqualTo(0.0);
    }

    @Test
    void shouldScoreZeroWhenAVectorHasZeroNorm() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0f, 0f});
        var candidate = FaceDescriptor.of(MODEL, new float[]{1f, 0f});

        assertThat(descriptor.cosineSimilarity(candidate).value()).isEqualTo(0.0);
        assertThat(candidate.cosineSimilarity(descriptor).value()).isEqualTo(0.0);
    }

    @Test
    void shouldComputeCosineForKnownVectors() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 0f});
        var candidate = FaceDescriptor.of(MODEL, new float[]{1f, 1f});

        assertThat(descriptor.cosineSimilarity(candidate).value()).isCloseTo(0.7071067811865475, within(1e-12));
    }

    @Test
    void shouldThrowWhenComparingDescriptorsOfDifferentModels() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 0f});
        var candidate = FaceDescriptor.of(OTHER_MODEL, new float[]{1f, 0f});

        assertThatThrownBy(() -> descriptor.cosineSimilarity(candidate)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenComparingDescriptorsOfDifferentDimensions() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 0f});
        var candidate = FaceDescriptor.of(MODEL, new float[]{1f, 0f, 0f});

        assertThatThrownBy(() -> descriptor.cosineSimilarity(candidate)).isInstanceOf(GuardException.class);
    }
}
