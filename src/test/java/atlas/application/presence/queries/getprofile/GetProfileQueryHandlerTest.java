package atlas.application.presence.queries.getprofile;

import static atlas.application.presence.support.PresenceApplicationTestData.PROFILE_ID;
import static atlas.application.presence.support.PresenceApplicationTestData.profile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.BiometricProfileRepository;
import atlas.domain.presence.PresenceErrors;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetProfileQueryHandlerTest {

    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final GetProfileQueryHandler handler = new GetProfileQueryHandler(profiles);

    @Test
    void shouldReturnTheProfileWithoutItsDescriptor() {
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.of(profile()));

        var result = handler.handle(new GetProfileQuery(PROFILE_ID));

        assertThat(result.value().id()).isEqualTo("B00000001");
        assertThat(result.value().displayName()).isEqualTo("Ada");
        assertThat(result.value().templates()).hasSize(1);
        assertThat(result.value().toString()).doesNotContain("1.0", "0.0");
    }

    @Test
    void shouldFailWhenProfileDoesNotExist() {
        when(profiles.get(PROFILE_ID)).thenReturn(Optional.empty());

        var result = handler.handle(new GetProfileQuery(PROFILE_ID));

        assertThat(result.error()).isEqualTo(PresenceErrors.profileNotFound(PROFILE_ID));
    }
}
