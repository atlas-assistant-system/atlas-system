package atlas.application.presence.commands.beginauthentication;

import static atlas.application.presence.support.PresenceApplicationTestData.CHALLENGE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.profile;
import static atlas.application.presence.support.PresenceApplicationTestData.wire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.services.LivenessPolicy;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BeginAuthenticationCommandHandlerTest {

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);
    private final LivenessChallengeRepository challenges = mock(LivenessChallengeRepository.class);
    private final BeginAuthenticationCommandHandler handler = new BeginAuthenticationCommandHandler(
        unitOfWork,
        challenges,
        new LivenessPolicy(),
        new Random(42),
        Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
        when(gates.get()).thenReturn(AuthenticationGate.initial());
    }

    @Test
    void shouldIssueAChallengeWhenAuthenticationCanBegin() {
        when(profiles.getAll()).thenReturn(List.of(profile()));
        when(challenges.nextId()).thenReturn(CHALLENGE_ID);

        var result = handler.handle(new BeginAuthenticationCommand());

        assertThat(result.value().challengeId()).isEqualTo("L00000001");
        assertThat(result.value().type()).isEqualTo("FIST");
        assertThat(result.value().nonce()).matches("[0-9a-f]{32}");
        assertThat(result.value().expiresAt()).isEqualTo(NOW.plusSeconds(10));
        verify(challenges).save(any());
    }

    @Test
    void shouldFailWhenNoProfilesAreEnrolled() {
        when(profiles.getAll()).thenReturn(List.of());

        var result = handler.handle(new BeginAuthenticationCommand());

        assertThat(result.error()).isEqualTo(PresenceErrors.NO_PROFILES_ENROLLED);
        verify(challenges, never()).save(any());
    }
}
