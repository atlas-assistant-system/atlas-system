package atlas.application.presence.queries.getauthenticationstate;

import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.profile;
import static atlas.application.presence.support.PresenceApplicationTestData.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.AuthenticationGateRepository;
import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.domain.presence.AuthenticationGate;
import atlas.domain.presence.enums.VerificationOutcome;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetAuthenticationStateQueryHandlerTest {

    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);

    @Test
    void shouldReturnTheCompleteAuthenticationState() {
        var gate = AuthenticationGate.initial();
        gate.registerFailure(VerificationOutcome.NO_MATCH, NOW);
        gate.registerFailure(VerificationOutcome.NO_MATCH, NOW);
        gate.registerFailure(VerificationOutcome.NO_MATCH, NOW);
        when(gates.get()).thenReturn(gate);
        when(profiles.getAll()).thenReturn(List.of(profile()));
        when(sessions.findActive()).thenReturn(List.of(session()));
        var handler = new GetAuthenticationStateQueryHandler(
            gates, profiles, sessions, Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC));

        var result = handler.handle(new GetAuthenticationStateQuery());

        assertThat(result.value().failedAttempts()).isEqualTo(3);
        assertThat(result.value().enrolledProfiles()).isEqualTo(1);
        assertThat(result.value().activeSession().id()).isEqualTo("S00000001");
    }

    @Test
    void shouldReturnNullWhenThereIsNoActiveSession() {
        when(gates.get()).thenReturn(AuthenticationGate.initial());
        when(profiles.getAll()).thenReturn(List.of());
        when(sessions.findActive()).thenReturn(List.of());
        var handler = new GetAuthenticationStateQueryHandler(
            gates, profiles, sessions, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = handler.handle(new GetAuthenticationStateQuery());

        assertThat(result.value().failedAttempts()).isZero();
        assertThat(result.value().enrolledProfiles()).isZero();
        assertThat(result.value().activeSession()).isNull();
    }
}
