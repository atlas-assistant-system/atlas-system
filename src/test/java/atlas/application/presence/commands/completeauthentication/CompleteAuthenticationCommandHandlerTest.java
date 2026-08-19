package atlas.application.presence.commands.completeauthentication;

import static atlas.application.presence.support.PresenceApplicationTestData.CHALLENGE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.MODEL;
import static atlas.application.presence.support.PresenceApplicationTestData.NONCE;
import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.SESSION_DURATION;
import static atlas.application.presence.support.PresenceApplicationTestData.SESSION_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.challenge;
import static atlas.application.presence.support.PresenceApplicationTestData.profile;
import static atlas.application.presence.support.PresenceApplicationTestData.wire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.AuthenticationGateRepository;
import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.application.presence.ports.LivenessChallengeRepository;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.domain.presence.AuthenticationGate;
import atlas.domain.presence.AuthenticationSession;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.services.FaceMatcher;
import atlas.domain.presence.vos.LivenessEvidence;
import atlas.domain.presence.vos.MatchThreshold;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteAuthenticationCommandHandlerTest {

    private static final java.time.Instant AUTHENTICATED_AT = NOW.plusSeconds(1);

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);
    private final LivenessChallengeRepository challenges = mock(LivenessChallengeRepository.class);

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
        when(gates.get()).thenReturn(AuthenticationGate.initial());
    }

    @Test
    void shouldOpenASessionAfterLivenessAndFaceMatch() {
        var gate = AuthenticationGate.initial();
        var profile = profile();
        when(gates.get()).thenReturn(gate);
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.of(challenge()));
        when(profiles.getAll()).thenReturn(List.of(profile));
        when(sessions.nextId()).thenReturn(SESSION_ID);

        var result = handlerAt(AUTHENTICATED_AT).handle(validCommand(new float[]{1.0f, 0.0f}));

        assertThat(result.value().id()).isEqualTo("S00000001");
        assertThat(result.value().profileId()).isEqualTo("B00000001");
        assertThat(result.value().openedAt()).isEqualTo(AUTHENTICATED_AT);
        assertThat(result.value().expiresAt()).isEqualTo(AUTHENTICATED_AT.plus(SESSION_DURATION.value()));
        assertThat(gate.failedAttempts()).isZero();

        var order = inOrder(gates, challenges, profiles, sessions);
        order.verify(gates).get();
        order.verify(challenges).get(CHALLENGE_ID);
        order.verify(profiles).getAll();
        order.verify(sessions).nextId();
        order.verify(sessions).create(any(AuthenticationSession.class));
        order.verify(gates).save(gate);
        order.verify(challenges).remove(CHALLENGE_ID);
    }

    @Test
    void shouldFailWhenChallengeDoesNotExist() {
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.empty());

        var result = handlerAt(AUTHENTICATED_AT).handle(validCommand(new float[]{1.0f, 0.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.challengeNotFound(CHALLENGE_ID));
        verify(challenges, never()).remove(any());
    }

    @Test
    void shouldRemoveChallengeWhenInputIsInvalid() {
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.of(challenge()));

        var invalidModel = handlerAt(AUTHENTICATED_AT).handle(new CompleteAuthenticationCommand(
            CHALLENGE_ID, " ", new float[]{1.0f}, "VICTORY", NONCE.value(), AUTHENTICATED_AT));

        assertThat(invalidModel.error()).isEqualTo(PresenceErrors.MODEL_VERSION_REQUIRED);
        verify(challenges).remove(CHALLENGE_ID);
        verify(gates, never()).save(any());
    }

    @Test
    void shouldRemoveChallengeWhenDescriptorIsInvalid() {
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.of(challenge()));

        var command = new CompleteAuthenticationCommand(
            CHALLENGE_ID, MODEL.value(), null, "VICTORY", NONCE.value(), AUTHENTICATED_AT);
        var result = handlerAt(AUTHENTICATED_AT).handle(command);

        assertThat(result.error()).isEqualTo(PresenceErrors.DESCRIPTOR_REQUIRED);
        verify(challenges).remove(CHALLENGE_ID);
        verify(gates, never()).save(any());
    }

    @Test
    void shouldCountMalformedEvidenceAsLivenessFailure() {
        var gate = AuthenticationGate.initial();
        when(gates.get()).thenReturn(gate);
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.of(challenge()));

        var command = new CompleteAuthenticationCommand(
            CHALLENGE_ID, MODEL.value(), new float[]{1.0f, 0.0f}, "UNKNOWN", "bad", AUTHENTICATED_AT);
        var result = handlerAt(AUTHENTICATED_AT).handle(command);

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_FAILED);
        assertThat(gate.failedAttempts()).isEqualTo(1);
        verify(gates).save(gate);
        verify(challenges).remove(CHALLENGE_ID);
    }

    @Test
    void shouldCountAnExpiredChallengeAsLivenessFailure() {
        var gate = AuthenticationGate.initial();
        when(gates.get()).thenReturn(gate);
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.of(challenge()));

        var result = handlerAt(NOW.plusSeconds(11)).handle(validCommand(new float[]{1.0f, 0.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_EXPIRED);
        assertThat(gate.failedAttempts()).isEqualTo(1);
        verify(gates).save(gate);
        verify(challenges).remove(CHALLENGE_ID);
    }

    @Test
    void shouldRemoveChallengeWhenNoProfilesRemain() {
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.of(challenge()));
        when(profiles.getAll()).thenReturn(List.of());

        var result = handlerAt(AUTHENTICATED_AT).handle(validCommand(new float[]{1.0f, 0.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.NO_PROFILES_ENROLLED);
        verify(challenges).remove(CHALLENGE_ID);
        verify(gates, never()).save(any());
    }

    @Test
    void shouldCountNoMatchAndNotOpenASession() {
        var gate = AuthenticationGate.initial();
        when(gates.get()).thenReturn(gate);
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.of(challenge()));
        when(profiles.getAll()).thenReturn(List.of(profile()));

        var result = handlerAt(AUTHENTICATED_AT).handle(validCommand(new float[]{20.0f, 20.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.VERIFICATION_NO_MATCH);
        assertThat(gate.failedAttempts()).isEqualTo(1);
        verify(gates).save(gate);
        verify(sessions, never()).create(any());
        verify(challenges).remove(CHALLENGE_ID);
    }

    @Test
    void shouldRejectAnAlreadyConsumedChallenge() {
        var gate = AuthenticationGate.initial();
        var challenge = challenge();
        challenge.consume(
            LivenessEvidence.of(NONCE, atlas.domain.presence.enums.LivenessChallengeType.VICTORY, AUTHENTICATED_AT),
            AUTHENTICATED_AT);
        when(gates.get()).thenReturn(gate);
        when(challenges.get(CHALLENGE_ID)).thenReturn(Optional.of(challenge));

        var result = handlerAt(NOW.plusSeconds(2)).handle(validCommand(new float[]{1.0f, 0.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.LIVENESS_CHALLENGE_ALREADY_USED);
        assertThat(gate.failedAttempts()).isEqualTo(1);
        verify(challenges).remove(CHALLENGE_ID);
    }

    private CompleteAuthenticationCommandHandler handlerAt(java.time.Instant now) {
        return new CompleteAuthenticationCommandHandler(
            unitOfWork,
            challenges,
            new FaceMatcher(),
            MatchThreshold.of(0.8),
            SESSION_DURATION,
            Clock.fixed(now, ZoneOffset.UTC));
    }

    private static CompleteAuthenticationCommand validCommand(float[] descriptor) {
        return new CompleteAuthenticationCommand(
            CHALLENGE_ID,
            MODEL.value(),
            descriptor,
            "VICTORY",
            NONCE.value(),
            AUTHENTICATED_AT);
    }

}
