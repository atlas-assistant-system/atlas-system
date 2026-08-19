package atlas.application.presence.commands.deleteprofile;

import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.PROFILE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.profile;
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

class DeleteProfileCommandHandlerTest {

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);
    private final DeleteProfileCommandHandler handler = new DeleteProfileCommandHandler(
        unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
    }

    @Test
    void shouldDeleteAProfileWithoutAnActiveSession() {
        var profile = profile();
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(sessions.findActiveByProfile(PROFILE_ID)).thenReturn(Optional.empty());

        var result = handler.handle(new DeleteProfileCommand(PROFILE_ID));

        assertThat(result.isSuccess()).isTrue();
        verify(profiles).delete(profile);
        verify(sessions, never()).update(any());
    }

    @Test
    void shouldCloseTheActiveSessionBeforeDeletingTheProfile() {
        var profile = profile();
        var session = session();
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.of(profile));
        when(sessions.findActiveByProfile(PROFILE_ID)).thenReturn(Optional.of(session));

        handler.handle(new DeleteProfileCommand(PROFILE_ID));

        assertThat(session.status()).isEqualTo(SessionStatus.CLOSED);
        verify(sessions).update(session);
        verify(profiles).delete(profile);
    }

    @Test
    void shouldFailWhenProfileDoesNotExist() {
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.empty());

        var result = handler.handle(new DeleteProfileCommand(PROFILE_ID));

        assertThat(result.error()).isEqualTo(PresenceErrors.profileNotFound(PROFILE_ID));
        verify(profiles, never()).delete(any());
        verify(sessions, never()).update(any());
    }
}
