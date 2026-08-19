package atlas.application.presence.commands.removefacetemplate;

import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.PROFILE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.TEMPLATE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.profile;
import static atlas.application.presence.support.PresenceApplicationTestData.profileWithTwoTemplates;
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

class RemoveFaceTemplateCommandHandlerTest {

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);
    private final RemoveFaceTemplateCommandHandler handler = new RemoveFaceTemplateCommandHandler(
        unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
    }

    @Test
    void shouldRemoveATemplateWhenAnotherRemains() {
        var profile = profileWithTwoTemplates();
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.of(profile));

        var result = handler.handle(new RemoveFaceTemplateCommand(PROFILE_ID, TEMPLATE_ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(profile.templates()).hasSize(1);
        verify(profiles).update(profile);
    }

    @Test
    void shouldFailWhenProfileDoesNotExist() {
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.empty());

        var result = handler.handle(new RemoveFaceTemplateCommand(PROFILE_ID, TEMPLATE_ID));

        assertThat(result.error()).isEqualTo(PresenceErrors.profileNotFound(PROFILE_ID));
        verify(profiles, never()).update(any());
    }

    @Test
    void shouldPropagateDomainFailure() {
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.of(profile()));

        var result = handler.handle(new RemoveFaceTemplateCommand(PROFILE_ID, TEMPLATE_ID));

        assertThat(result.error()).isEqualTo(PresenceErrors.LAST_TEMPLATE_CANNOT_BE_REMOVED);
        verify(profiles, never()).update(any());
    }
}
