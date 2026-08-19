package atlas.application.presence.queries.listprofiles;

import static atlas.application.presence.support.PresenceApplicationTestData.profile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.BiometricProfileRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListProfilesQueryHandlerTest {

    private final BiometricProfileRepository profiles = mock(BiometricProfileRepository.class);
    private final ListProfilesQueryHandler handler = new ListProfilesQueryHandler(profiles);

    @Test
    void shouldReturnProfileSummaries() {
        when(profiles.getAll()).thenReturn(List.of(profile()));

        var result = handler.handle(new ListProfilesQuery());

        assertThat(result.value()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo("B00000001");
            assertThat(summary.displayName()).isEqualTo("Ada");
            assertThat(summary.templateCount()).isEqualTo(1);
        });
    }

    @Test
    void shouldReturnAnEmptyListWhenNoProfilesExist() {
        when(profiles.getAll()).thenReturn(List.of());

        assertThat(handler.handle(new ListProfilesQuery()).value()).isEmpty();
    }
}
