package atlas.domain.presence.vos;

import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.util.Arrays;
import java.util.Objects;

public record FaceDescriptor(ModelVersion modelVersion, float[] values) implements ValueObject {

    public FaceDescriptor {
        ObjectGuard.notNull(modelVersion, "modelVersion");
        ObjectGuard.notNull(values, "values");

        if (values.length == 0) {
            throw GuardException.forParameter("values", "cannot be empty");
        }

        values = values.clone();
    }

    public static Result<FaceDescriptor> create(ModelVersion modelVersion, float[] values) {
        if (modelVersion == null) {
            return Result.failure(PresenceErrors.MODEL_VERSION_REQUIRED);
        }

        if (values == null || values.length == 0) {
            return Result.failure(PresenceErrors.DESCRIPTOR_REQUIRED);
        }

        return Result.success(new FaceDescriptor(modelVersion, values));
    }

    public static FaceDescriptor of(ModelVersion modelVersion, float[] values) {
        return new FaceDescriptor(modelVersion, values);
    }

    @Override
    public float[] values() {
        return values.clone();
    }

    public int dimension() {
        return values.length;
    }

    public boolean isComparableWith(FaceDescriptor other) {
        ObjectGuard.notNull(other, "other");

        return modelVersion.equals(other.modelVersion) && values.length == other.values.length;
    }

    public SimilarityScore similarity(FaceDescriptor other) {
        if (!isComparableWith(other)) {
            throw GuardException.forParameter("other", "must share model version and dimension");
        }

        var squaredDistance = 0.0;
        for (var i = 0; i < values.length; i++) {
            var difference = (double) values[i] - other.values[i];
            squaredDistance += difference * difference;
        }

        if (squaredDistance == 0.0) {
            return SimilarityScore.of(1.0);
        }

        var distance = Math.round(100.0 * 25.0 * squaredDistance) / 100.0;
        var normalized = (1.0 - Math.sqrt(distance) / 100.0 - 0.2) / (0.8 - 0.2);
        var score = Math.round(100.0 * Math.clamp(normalized, 0.0, 1.0)) / 100.0;

        return SimilarityScore.of(score);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FaceDescriptor that
            && modelVersion.equals(that.modelVersion)
            && Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelVersion, Arrays.hashCode(values));
    }

    @Override
    public String toString() {
        return "FaceDescriptor[model=" + modelVersion + ", dimension=" + values.length + "]";
    }
}
