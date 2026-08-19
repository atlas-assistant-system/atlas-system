package atlas.domain.presence.vos;

import atlas.domain.presence.PresenceErrors;
import atlas.domain.sharedkernel.ddd.SingleValueObject;
import atlas.domain.sharedkernel.guards.StringGuard;
import atlas.domain.sharedkernel.results.Result;

public record ModelVersion(String value) implements SingleValueObject<String> {

    public ModelVersion {
        StringGuard.notBlank(value, "value");
    }

    public static Result<ModelVersion> create(String value) {
        if (value == null || value.isBlank()) {
            return Result.failure(PresenceErrors.MODEL_VERSION_REQUIRED);
        }

        return Result.success(new ModelVersion(value));
    }

    public static ModelVersion of(String value) {
        return new ModelVersion(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
