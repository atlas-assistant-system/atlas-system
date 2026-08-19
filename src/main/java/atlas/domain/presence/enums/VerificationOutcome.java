package atlas.domain.presence.enums;

public enum VerificationOutcome {

    MATCHED,
    NO_MATCH,
    LIVENESS_FAILED,
    NO_PROFILES_ENROLLED;

    public boolean succeeded() {
        return this == MATCHED;
    }

    public boolean isFailedVerification() {
        return this == NO_MATCH || this == LIVENESS_FAILED;
    }
}
