package atlas.domain.presence;

import atlas.domain.presence.entities.FaceTemplateId;
import sharedkernel.domain.results.Error;

public final class PresenceErrors {

    public static final Error PROFILE_NAME_REQUIRED =
        Error.validation("Profile.NameRequired", "A profile name is required.");

    public static final Error PROFILE_NAME_TOO_LONG =
        Error.validation("Profile.NameTooLong", "The profile name is too long.");

    public static final Error DESCRIPTOR_REQUIRED =
        Error.validation("Profile.DescriptorRequired", "A face descriptor is required.");

    public static final Error MODEL_VERSION_REQUIRED =
        Error.validation("Profile.ModelVersionRequired", "A model version is required.");

    public static final Error INVALID_SIMILARITY_SCORE =
        Error.validation("Verification.InvalidSimilarityScore", "A similarity score must be between 0.0 and 1.0.");

    public static final Error INVALID_MATCH_THRESHOLD =
        Error.validation("Verification.InvalidMatchThreshold", "A match threshold must be between 0.0 and 1.0.");

    public static final Error INVALID_SESSION_DURATION =
        Error.validation("Session.InvalidDuration", "The session duration is out of range.");

    public static final Error DESCRIPTOR_DIMENSION_MISMATCH = Error.validation(
        "Profile.DescriptorDimensionMismatch",
        "The descriptor dimension does not match its model version.");

    public static final Error NO_PROFILES_ENROLLED =
        Error.conflict("Authentication.NoProfilesEnrolled", "No biometric profiles are enrolled.");

    public static final Error TOO_MANY_TEMPLATES = Error.conflict(
        "Profile.TooManyTemplates", "The profile already has the maximum number of face templates.");

    public static final Error LAST_TEMPLATE_CANNOT_BE_REMOVED = Error.conflict(
        "Profile.LastTemplateCannotBeRemoved", "The last face template of a profile cannot be removed.");

    public static final Error MODEL_VERSION_MISMATCH = Error.conflict(
        "Profile.ModelVersionMismatch",
        "The descriptor was produced by a different model version than the profile's templates.");

    public static final Error LIVENESS_CHALLENGE_EXPIRED =
        Error.conflict("Liveness.ChallengeExpired", "The liveness challenge has expired.");

    public static final Error LIVENESS_CHALLENGE_ALREADY_USED =
        Error.conflict("Liveness.ChallengeAlreadyUsed", "The liveness challenge has already been used.");

    public static final Error LIVENESS_FAILED =
        Error.unauthorized("Liveness.Failed", "The liveness check was not passed.");

    public static final Error VERIFICATION_NO_MATCH =
        Error.unauthorized("Verification.NoMatch", "The face does not match any enrolled profile.");

    public static final Error SESSION_EXPIRED =
        Error.unauthorized("Session.Expired", "The session has expired.");

    public static final Error SESSION_CLOSED =
        Error.conflict("Session.Closed", "The session has been closed.");

    public static final Error INTERACTION_REQUIRES_SESSION = Error.unauthorized(
        "Interaction.RequiresSession", "An active session is required to accept interaction.");

    public static final Error MAINTENANCE_MODE_REQUIRED =
        Error.forbidden("Presence.MaintenanceModeRequired", "This operation requires maintenance mode.");

    public static Error profileNotFound(BiometricProfileId id) {
        return Error.notFound("Profile.NotFound", "Profile '" + id + "' was not found.");
    }

    public static Error templateNotFound(FaceTemplateId id) {
        return Error.notFound(
            "Profile.TemplateNotFound", "Face template '" + id + "' was not found in this profile.");
    }

    public static Error challengeNotFound(LivenessChallengeId id) {
        return Error.notFound("Liveness.ChallengeNotFound", "Liveness challenge '" + id + "' was not found.");
    }

    public static Error sessionNotFound(SessionId id) {
        return Error.notFound("Session.NotFound", "Session '" + id + "' was not found.");
    }

    private PresenceErrors() {}
}
