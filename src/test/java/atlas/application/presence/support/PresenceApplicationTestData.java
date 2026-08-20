package atlas.application.presence.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.AuthenticationGateRepository;
import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.domain.presence.AuthenticationSession;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.LivenessChallenge;
import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.SessionId;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.presence.enums.LivenessChallengeType;
import atlas.domain.presence.vos.ChallengeNonce;
import atlas.domain.presence.vos.FaceDescriptor;
import atlas.domain.presence.vos.ModelVersion;
import atlas.domain.presence.vos.ProfileName;
import atlas.domain.presence.vos.SessionDuration;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public final class PresenceApplicationTestData {

    public static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");
    public static final BiometricProfileId PROFILE_ID = BiometricProfileId.of(1);
    public static final FaceTemplateId TEMPLATE_ID = FaceTemplateId.of(new UUID(0, 1));
    public static final FaceTemplateId SECOND_TEMPLATE_ID = FaceTemplateId.of(new UUID(0, 2));
    public static final SessionId SESSION_ID = SessionId.of(1);
    public static final LivenessChallengeId CHALLENGE_ID = LivenessChallengeId.of(1);
    public static final ModelVersion MODEL = ModelVersion.of("face-v1");
    public static final ChallengeNonce NONCE = ChallengeNonce.of("00112233445566778899aabbccddeeff");
    public static final SessionDuration SESSION_DURATION = SessionDuration.of(Duration.ofMinutes(15));

    private PresenceApplicationTestData() {}

    public static BiometricProfile profile() {
        return BiometricProfile.enroll(
            PROFILE_ID,
            ProfileName.of("Ada"),
            TEMPLATE_ID,
            FaceDescriptor.of(MODEL, new float[]{1.0f, 0.0f}),
            NOW);
    }

    public static BiometricProfile profileWithTwoTemplates() {
        var profile = profile();
        profile.addTemplate(
            SECOND_TEMPLATE_ID, FaceDescriptor.of(MODEL, new float[]{0.9f, 0.1f}), NOW);
        profile.clearEvents();
        return profile;
    }

    public static AuthenticationSession session() {
        return AuthenticationSession.open(SESSION_ID, PROFILE_ID, SESSION_DURATION, NOW);
    }

    public static LivenessChallenge challenge() {
        return LivenessChallenge.issue(CHALLENGE_ID, LivenessChallengeType.FIST, NONCE, NOW);
    }

    public static void wire(
        PresenceUnitOfWork unitOfWork,
        BiometricProfileRepository profiles,
        AuthenticationSessionRepository sessions,
        AuthenticationGateRepository gates) {
        when(unitOfWork.profiles()).thenReturn(profiles);
        when(unitOfWork.sessions()).thenReturn(sessions);
        when(unitOfWork.gate()).thenReturn(gates);
        when(unitOfWork.execute(any())).thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(0).get());
    }
}
