package atlas.application.presence.commands.closesession;

import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
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
import atlas.domain.presence.enums.SessionStatus;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CloseSessionCommandHandlerTest {

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);
    private final CloseSessionCommandHandler handler = new CloseSessionCommandHandler(
        unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
    }

    @Test
    void shouldCloseAnExistingSession() {
        var session = session();
        when(sessions.get(SESSION_ID)).thenReturn(Optional.of(session));

        var result = handler.handle(new CloseSessionCommand(SESSION_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(session.status()).isEqualTo(SessionStatus.CLOSED);
        verify(sessions).update(session);
    }

    @Test
    void shouldRemainSuccessfulWhenSessionIsAlreadyClosed() {
        var session = session();
        session.close(NOW);
        when(sessions.get(SESSION_ID)).thenReturn(Optional.of(session));

        var result = handler.handle(new CloseSessionCommand(SESSION_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(session.status()).isEqualTo(SessionStatus.CLOSED);
        verify(sessions).update(session);
    }

    @Test
    void shouldFailWhenSessionDoesNotExist() {
        when(sessions.get(SESSION_ID)).thenReturn(Optional.empty());

        var result = handler.handle(new CloseSessionCommand(SESSION_ID));

        assertThat(result.error()).isEqualTo(PresenceErrors.sessionNotFound(SESSION_ID));
        verify(sessions, never()).update(any());
    }
}
