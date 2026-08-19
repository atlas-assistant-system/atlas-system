package atlas.application.presence.mappers;

import atlas.application.presence.dto.AuthenticationAttemptDto;
import atlas.application.presence.dto.ChallengeDto;
import atlas.application.presence.dto.FaceTemplateDto;
import atlas.application.presence.dto.ProfileDto;
import atlas.application.presence.dto.ProfileSummaryDto;
import atlas.application.presence.dto.SessionDto;
import atlas.application.presence.ports.AuthenticationAttempt;
import atlas.domain.presence.AuthenticationSession;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.LivenessChallenge;
import atlas.domain.presence.entities.FaceTemplate;

public final class PresenceMapper {

    private PresenceMapper() {}

    public static ProfileDto toDto(BiometricProfile profile) {
        return new ProfileDto(
            profile.id().toString(),
            profile.displayName().value(),
            profile.modelVersion().value(),
            profile.templates().size(),
            profile.templates().stream().map(PresenceMapper::toDto).toList());
    }

    public static ProfileSummaryDto toSummaryDto(BiometricProfile profile) {
        return new ProfileSummaryDto(
            profile.id().toString(), profile.displayName().value(), profile.templates().size());
    }

    public static FaceTemplateDto toDto(FaceTemplate template) {
        return new FaceTemplateDto(template.id().toString(), template.capturedAt());
    }

    public static ChallengeDto toDto(LivenessChallenge challenge) {
        return new ChallengeDto(
            challenge.id().toString(),
            challenge.type().name(),
            challenge.nonce().value(),
            challenge.expiresAt());
    }

    public static SessionDto toDto(AuthenticationSession session) {
        return new SessionDto(
            session.id().toString(),
            session.profileId().toString(),
            session.openedAt(),
            session.lastActivityAt(),
            session.expiresAt(),
            session.status().name());
    }

    public static AuthenticationAttemptDto toDto(AuthenticationAttempt attempt) {
        return new AuthenticationAttemptDto(attempt.occurredOn(), attempt.outcome().name());
    }
}
