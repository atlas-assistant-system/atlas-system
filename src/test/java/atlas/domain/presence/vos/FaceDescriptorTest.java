package atlas.domain.presence.vos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

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

        assertThat(descriptor.similarity(identical).value()).isEqualTo(1.0);
    }

    @Test
    void shouldUseHumansNormalizedEuclideanSimilarity() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 0f});
        var candidate = FaceDescriptor.of(MODEL, new float[]{9.8f, 0f});

        assertThat(descriptor.similarity(candidate).value()).isEqualTo(0.6);
    }

    @Test
    void shouldClampLargeDistancesToZero() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{0f, 0f});
        var distant = FaceDescriptor.of(MODEL, new float[]{20f, 0f});

        assertThat(descriptor.similarity(distant).value()).isEqualTo(0.0);
    }

    @Test
    void shouldBeSymmetric() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 2f});
        var candidate = FaceDescriptor.of(MODEL, new float[]{7f, 9f});

        assertThat(descriptor.similarity(candidate)).isEqualTo(candidate.similarity(descriptor));
    }

    @Test
    void shouldThrowWhenComparingDescriptorsOfDifferentModels() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 0f});
        var candidate = FaceDescriptor.of(OTHER_MODEL, new float[]{1f, 0f});

        assertThatThrownBy(() -> descriptor.similarity(candidate)).isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenComparingDescriptorsOfDifferentDimensions() {
        var descriptor = FaceDescriptor.of(MODEL, new float[]{1f, 0f});
        var candidate = FaceDescriptor.of(MODEL, new float[]{1f, 0f, 0f});

        assertThatThrownBy(() -> descriptor.similarity(candidate)).isInstanceOf(GuardException.class);
    }
}
