package atlas.application.presence.commands.addfacetemplate;

import static atlas.application.presence.support.PresenceApplicationTestData.MODEL;
import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.PROFILE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.SECOND_TEMPLATE_ID;
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
import atlas.application.presence.ports.FaceTemplateIdGenerator;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.domain.presence.PresenceErrors;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddFaceTemplateCommandHandlerTest {

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);
    private final FaceTemplateIdGenerator templateIds = mock(FaceTemplateIdGenerator.class);
    private final AddFaceTemplateCommandHandler handler = new AddFaceTemplateCommandHandler(
        unitOfWork, templateIds, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
        when(templateIds.next()).thenReturn(SECOND_TEMPLATE_ID);
    }

    @Test
    void shouldAddACompatibleTemplate() {
        var profile = profile();
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.of(profile));

        var result = handler.handle(
            new AddFaceTemplateCommand(PROFILE_ID, MODEL.value(), new float[]{0.9f, 0.1f}));

        assertThat(result.value().templateCount()).isEqualTo(2);
        assertThat(result.value().templates()).hasSize(2);
        verify(profiles).update(profile);
    }

    @Test
    void shouldFailWhenProfileDoesNotExist() {
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.empty());

        var result = handler.handle(new AddFaceTemplateCommand(PROFILE_ID, MODEL.value(), new float[]{1.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.profileNotFound(PROFILE_ID));
        verify(profiles, never()).update(any());
    }

    @Test
    void shouldPropagateInvalidInput() {
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.of(profile()));

        var invalidModel = handler.handle(new AddFaceTemplateCommand(PROFILE_ID, " ", new float[]{1.0f}));
        var invalidDescriptor = handler.handle(new AddFaceTemplateCommand(PROFILE_ID, MODEL.value(), null));

        assertThat(invalidModel.error()).isEqualTo(PresenceErrors.MODEL_VERSION_REQUIRED);
        assertThat(invalidDescriptor.error()).isEqualTo(PresenceErrors.DESCRIPTOR_REQUIRED);
        verify(profiles, never()).update(any());
    }

    @Test
    void shouldPropagateDomainFailure() {
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.of(profile()));

        var result = handler.handle(
            new AddFaceTemplateCommand(PROFILE_ID, "face-v2", new float[]{1.0f, 0.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.MODEL_VERSION_MISMATCH);
        verify(profiles, never()).update(any());
    }
}
