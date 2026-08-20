package atlas.application.presence.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.presence.ports.AuthenticationAttempt;
import atlas.domain.presence.AuthenticationSession;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.LivenessChallenge;
import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.SessionId;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.presence.enums.VerificationOutcome;
import atlas.domain.presence.vos.ChallengeNonce;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.ProfileName;
import atlas.domain.presence.vos.SessionDuration;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PresenceMapperTest {

    private static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");
    private static final BiometricProfileId PROFILE_ID = BiometricProfileId.of(1);
    private static final FaceTemplateId TEMPLATE_ID = FaceTemplateId.of(new UUID(0, 1));
    private static final ModelVersion MODEL = ModelVersion.of("face-v1");

    @Test
    void shouldMapEveryProfileFieldWithoutExposingTheDescriptor() {
        var profile = profile();

        var dto = PresenceMapper.toDto(profile);

        assertThat(dto.id()).isEqualTo("B00000001");
        assertThat(dto.displayName()).isEqualTo("Ada");
        assertThat(dto.modelVersion()).isEqualTo("face-v1");
        assertThat(dto.templateCount()).isEqualTo(1);
        assertThat(dto.templates()).singleElement().satisfies(template -> {
            assertThat(template.id()).isEqualTo(TEMPLATE_ID.toString());
            assertThat(template.capturedAt()).isEqualTo(NOW);
        });
        assertThat(dto.toString()).doesNotContain("0.1234567", "0.7654321");
    }

    @Test
    void shouldMapAProfileSummary() {
        var dto = PresenceMapper.toSummaryDto(profile());

        assertThat(dto.id()).isEqualTo("B00000001");
        assertThat(dto.displayName()).isEqualTo("Ada");
        assertThat(dto.templateCount()).isEqualTo(1);
    }

    @Test
    void shouldMapAChallenge() {
        var challenge = LivenessChallenge.issue(
            LivenessChallengeId.of(1),
            LivenessChallengeType.FIST,
            ChallengeNonce.of("00112233445566778899aabbccddeeff"),
            NOW);

        var dto = PresenceMapper.toDto(challenge);

        assertThat(dto.challengeId()).isEqualTo("L00000001");
        assertThat(dto.type()).isEqualTo("FIST");
        assertThat(dto.nonce()).isEqualTo("00112233445566778899aabbccddeeff");
        assertThat(dto.expiresAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void shouldMapASession() {
        var session = AuthenticationSession.open(
            SessionId.of(1), PROFILE_ID, SessionDuration.of(Duration.ofMinutes(15)), NOW);

        var dto = PresenceMapper.toDto(session);

        assertThat(dto.id()).isEqualTo("S00000001");
        assertThat(dto.profileId()).isEqualTo("B00000001");
        assertThat(dto.openedAt()).isEqualTo(NOW);
        assertThat(dto.lastActivityAt()).isEqualTo(NOW);
        assertThat(dto.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(dto.status()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldMapAnAuthenticationAttempt() {
        var dto = PresenceMapper.toDto(new AuthenticationAttempt(NOW, VerificationOutcome.NO_MATCH));

        assertThat(dto.occurredOn()).isEqualTo(NOW);
        assertThat(dto.outcome()).isEqualTo("NO_MATCH");
    }

    private static BiometricProfile profile() {
        return BiometricProfile.enroll(
            PROFILE_ID,
            ProfileName.of("Ada"),
            TEMPLATE_ID,
            FaceDescriptor.of(MODEL, new float[]{0.1234567f, 0.7654321f}),
            NOW);
    }
}
