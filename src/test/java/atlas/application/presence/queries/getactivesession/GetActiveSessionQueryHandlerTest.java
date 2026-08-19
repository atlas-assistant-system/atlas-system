package atlas.application.presence.queries.getactivesession;

import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static atlas.application.presence.support.PresenceApplicationTestData.session;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.AuthenticationSessionRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetActiveSessionQueryHandlerTest {

    private final AuthenticationSessionRepository sessions = mock(AuthenticationSessionRepository.class);

    @Test
    void shouldReturnAnActiveSession() {
        when(sessions.findActive()).thenReturn(List.of(session()));
        var handler = new GetActiveSessionQueryHandler(sessions, Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC));

        var result = handler.handle(new GetActiveSessionQuery());

        assertThat(result.value()).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("S00000001");
            assertThat(dto.status()).isEqualTo("ACTIVE");
        });
    }

    @Test
    void shouldIgnoreAStaleSessionStillMarkedActive() {
        var session = session();
        when(sessions.findActive()).thenReturn(List.of(session));
        var handler = new GetActiveSessionQueryHandler(sessions, Clock.fixed(session.expiresAt(), ZoneOffset.UTC));

        assertThat(handler.handle(new GetActiveSessionQuery()).value()).isEmpty();
    }
}
