package atlas.application.presence.commands.expirestalesessions;

import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.session;
import static atlas.application.presence.support.PresenceApplicationTestData.wire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.AuthenticationGateRepository;
import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.domain.presence.enums.SessionStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpireStaleSessionsCommandHandlerTest {

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
    }

    @Test
    void shouldExpireOnlyStaleSessions() {
        var stale = session();
        var active = session();
        var atExpiry = stale.expiresAt();
        when(sessions.findActive()).thenReturn(List.of(stale, active));
        var handler = new ExpireStaleSessionsCommandHandler(
            unitOfWork, Clock.fixed(atExpiry.minusSeconds(1), ZoneOffset.UTC));

        var none = handler.handle(new ExpireStaleSessionsCommand());

        assertThat(none.value()).isZero();
        verify(sessions, never()).update(org.mockito.ArgumentMatchers.any());

        var expiryHandler = new ExpireStaleSessionsCommandHandler(
            unitOfWork, Clock.fixed(atExpiry, ZoneOffset.UTC));
        var expired = expiryHandler.handle(new ExpireStaleSessionsCommand());

        assertThat(expired.value()).isEqualTo(2);
        assertThat(stale.status()).isEqualTo(SessionStatus.EXPIRED);
        assertThat(active.status()).isEqualTo(SessionStatus.EXPIRED);
        verify(sessions, times(2)).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnZeroWhenThereAreNoActiveSessions() {
        when(sessions.findActive()).thenReturn(List.of());
        var handler = new ExpireStaleSessionsCommandHandler(
            unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = handler.handle(new ExpireStaleSessionsCommand());

        assertThat(result.value()).isZero();
    }
}
