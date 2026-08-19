package atlas.domain.presence.vos;

import atlas.domain.presence.PresenceErrors;
import sharedkernel.domain.ddd.SingleValueObject;
import sharedkernel.domain.guards.StringGuard;
import sharedkernel.domain.results.Result;

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
