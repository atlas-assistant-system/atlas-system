package atlas.application.presence.commands.refreshsession;

import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.SESSION_DURATION;
import static atlas.application.presence.support.PresenceApplicationTestData.SESSION_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.session;
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
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.domain.presence.PresenceErrors;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshSessionCommandHandlerTest {

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
    }

    @Test
    void shouldRefreshAnActiveSession() {
        var session = session();
        var later = NOW.plusSeconds(60);
        when(sessions.get(SESSION_ID)).thenReturn(Optional.of(session));
        var handler = new RefreshSessionCommandHandler(
            unitOfWork, SESSION_DURATION, Clock.fixed(later, ZoneOffset.UTC));

        var result = handler.handle(new RefreshSessionCommand(SESSION_ID));

        assertThat(result.value().lastActivityAt()).isEqualTo(later);
        assertThat(result.value().expiresAt()).isEqualTo(later.plus(SESSION_DURATION.value()));
        verify(sessions).update(session);
    }

    @Test
    void shouldFailWhenSessionDoesNotExist() {
        when(sessions.get(SESSION_ID)).thenReturn(Optional.empty());
        var handler = new RefreshSessionCommandHandler(
            unitOfWork, SESSION_DURATION, Clock.fixed(NOW, ZoneOffset.UTC));

        var result = handler.handle(new RefreshSessionCommand(SESSION_ID));

        assertThat(result.error()).isEqualTo(PresenceErrors.sessionNotFound(SESSION_ID));
        verify(sessions, never()).update(any());
    }

    @Test
    void shouldPropagateAnExpiredSession() {
        var session = session();
        when(sessions.get(SESSION_ID)).thenReturn(Optional.of(session));
        var handler = new RefreshSessionCommandHandler(
            unitOfWork, SESSION_DURATION, Clock.fixed(session.expiresAt(), ZoneOffset.UTC));

        var result = handler.handle(new RefreshSessionCommand(SESSION_ID));

        assertThat(result.error()).isEqualTo(PresenceErrors.SESSION_EXPIRED);
        verify(sessions, never()).update(any());
    }
}
