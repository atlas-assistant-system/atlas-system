package atlas.domain.presence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.presence.entities.FaceTemplateId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import sharedkernel.domain.results.ErrorType;

class PresenceErrorsTest {

    @Test
    void shouldNameTheProfileThatWasNotFound() {
        var error = PresenceErrors.profileNotFound(BiometricProfileId.of(7));

        assertThat(error.code()).isEqualTo("Profile.NotFound");
        assertThat(error.type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(error.message()).contains("B00000007");
    }

    @Test
    void shouldNameTheTemplateThatWasNotFound() {
        var id = FaceTemplateId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        var error = PresenceErrors.templateNotFound(id);

        assertThat(error.code()).isEqualTo("Profile.TemplateNotFound");
        assertThat(error.type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(error.message()).contains("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void shouldNameTheChallengeThatWasNotFound() {
        var error = PresenceErrors.challengeNotFound(LivenessChallengeId.of(7));

        assertThat(error.code()).isEqualTo("Liveness.ChallengeNotFound");
        assertThat(error.type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(error.message()).contains("L00000007");
    }

    @Test
    void shouldNameTheSessionThatWasNotFound() {
        var error = PresenceErrors.sessionNotFound(SessionId.of(7));

        assertThat(error.code()).isEqualTo("Session.NotFound");
        assertThat(error.type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(error.message()).contains("S00000007");
    }

    @Test
    void shouldClassifyInvalidInputAsValidation() {
        assertThat(PresenceErrors.PROFILE_NAME_REQUIRED.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(PresenceErrors.PROFILE_NAME_TOO_LONG.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(PresenceErrors.DESCRIPTOR_REQUIRED.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(PresenceErrors.MODEL_VERSION_REQUIRED.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(PresenceErrors.INVALID_SIMILARITY_SCORE.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(PresenceErrors.INVALID_MATCH_THRESHOLD.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(PresenceErrors.INVALID_SESSION_DURATION.type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(PresenceErrors.DESCRIPTOR_DIMENSION_MISMATCH.type()).isEqualTo(ErrorType.VALIDATION);
    }

    @Test
    void shouldClassifyStateConflictsApartFromInvalidInput() {
        assertThat(PresenceErrors.NO_PROFILES_ENROLLED.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(PresenceErrors.TOO_MANY_TEMPLATES.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(PresenceErrors.LAST_TEMPLATE_CANNOT_BE_REMOVED.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(PresenceErrors.MODEL_VERSION_MISMATCH.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(PresenceErrors.LIVENESS_CHALLENGE_ALREADY_USED.type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(PresenceErrors.SESSION_CLOSED.type()).isEqualTo(ErrorType.CONFLICT);
    }

    @Test
    void shouldClassifyRefusedAuthenticationAsUnauthorized() {
        assertThat(PresenceErrors.LIVENESS_FAILED.type()).isEqualTo(ErrorType.UNAUTHORIZED);
        assertThat(PresenceErrors.VERIFICATION_NO_MATCH.type()).isEqualTo(ErrorType.UNAUTHORIZED);
        assertThat(PresenceErrors.SESSION_EXPIRED.type()).isEqualTo(ErrorType.UNAUTHORIZED);
        assertThat(PresenceErrors.INTERACTION_REQUIRES_SESSION.type()).isEqualTo(ErrorType.UNAUTHORIZED);
    }

    @Test
    void shouldClassifyMaintenanceOnlyOperationsAsForbidden() {
        assertThat(PresenceErrors.MAINTENANCE_MODE_REQUIRED.type()).isEqualTo(ErrorType.FORBIDDEN);
    }

    @Test
    void shouldGroupErrorCodesByConcept() {
        assertThat(PresenceErrors.PROFILE_NAME_REQUIRED.code()).isEqualTo("Profile.NameRequired");
        assertThat(PresenceErrors.NO_PROFILES_ENROLLED.code()).isEqualTo("Authentication.NoProfilesEnrolled");
        assertThat(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED.code()).isEqualTo("Liveness.ChallengeExpired");
        assertThat(PresenceErrors.SESSION_EXPIRED.code()).isEqualTo("Session.Expired");
        assertThat(PresenceErrors.VERIFICATION_NO_MATCH.code()).isEqualTo("Verification.NoMatch");
        assertThat(PresenceErrors.INTERACTION_REQUIRES_SESSION.code()).isEqualTo("Interaction.RequiresSession");
        assertThat(PresenceErrors.MAINTENANCE_MODE_REQUIRED.code()).isEqualTo("Presence.MaintenanceModeRequired");
    }
}
