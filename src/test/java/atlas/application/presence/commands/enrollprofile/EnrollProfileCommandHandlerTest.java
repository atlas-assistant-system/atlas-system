package atlas.application.presence.commands.enrollprofile;

import static atlas.application.presence.support.PresenceApplicationTestData.MODEL;
import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.PROFILE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.TEMPLATE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.wire;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.AuthenticationGateRepository;
import atlas.application.presence.ports.AuthenticationSessionRepository;
import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.application.presence.ports.FaceTemplateIdGenerator;
import atlas.application.presence.ports.PresenceUnitOfWork;
import atlas.domain.presence.BiometricProfile;
import atlas.domain.presence.PresenceErrors;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrollProfileCommandHandlerTest {

    private final PresenceUnitOfWork unitOfWork = mock(PresenceUnitOfWork.class);
    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);
    private final AuthenticationGateRepository gates = mock(AuthenticationGateRepository.class);
    private final FaceTemplateIdGenerator templateIds = mock(FaceTemplateIdGenerator.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void wireUnitOfWork() {
        wire(unitOfWork, profiles, sessions, gates);
        when(profiles.nextId()).thenReturn(PROFILE_ID);
        when(templateIds.next()).thenReturn(TEMPLATE_ID);
    }

    @Test
    void shouldEnrollAProfileInMaintenanceMode() {
        var handler = new EnrollProfileCommandHandler(unitOfWork, templateIds, clock, true);

        var result = handler.handle(new EnrollProfileCommand(" Ada ", MODEL.value(), new float[]{1.0f, 0.0f}));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().id()).isEqualTo("B00000001");
        assertThat(result.value().displayName()).isEqualTo("Ada");
        assertThat(result.value().modelVersion()).isEqualTo("face-v1");
        assertThat(result.value().templateCount()).isEqualTo(1);
        verify(profiles).create(org.mockito.ArgumentMatchers.any(BiometricProfile.class));
    }

    @Test
    void shouldRequireAnActiveSessionOutsideMaintenanceMode() {
        var handler = new EnrollProfileCommandHandler(unitOfWork, templateIds, clock, false);

        var result = handler.handle(new EnrollProfileCommand("Ada", MODEL.value(), new float[]{1.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.INTERACTION_REQUIRES_SESSION);
        verify(profiles, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldEnrollWithAnActiveSessionOutsideMaintenanceMode() {
        when(sessions.findActive())
            .thenReturn(List.of(atlas.application.presence.support.PresenceApplicationTestData.session()));
        var handler = new EnrollProfileCommandHandler(unitOfWork, templateIds, clock, false);

        var result = handler.handle(new EnrollProfileCommand("Ada", MODEL.value(), new float[]{1.0f}));

        assertThat(result.isSuccess()).isTrue();
        verify(profiles).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPropagateInvalidProfileName() {
        var handler = new EnrollProfileCommandHandler(unitOfWork, templateIds, clock, true);

        var result = handler.handle(new EnrollProfileCommand(" ", MODEL.value(), new float[]{1.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.PROFILE_NAME_REQUIRED);
        verify(profiles, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPropagateInvalidModelVersion() {
        var handler = new EnrollProfileCommandHandler(unitOfWork, templateIds, clock, true);

        var result = handler.handle(new EnrollProfileCommand("Ada", " ", new float[]{1.0f}));

        assertThat(result.error()).isEqualTo(PresenceErrors.MODEL_VERSION_REQUIRED);
        verify(profiles, never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPropagateInvalidDescriptor() {
        var handler = new EnrollProfileCommandHandler(unitOfWork, templateIds, clock, true);

        var result = handler.handle(new EnrollProfileCommand("Ada", MODEL.value(), null));

        assertThat(result.error()).isEqualTo(PresenceErrors.DESCRIPTOR_REQUIRED);
        verify(profiles, never()).create(org.mockito.ArgumentMatchers.any());
    }
}
