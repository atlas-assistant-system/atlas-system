package atlas.application.presence.queries.listauthenticationattempts;

import static atlas.application.presence.support.PresenceApplicationTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.AuthenticationAttempt;
import atlas.application.presence.ports.AuthenticationAttemptReadModel;
import atlas.domain.presence.enums.VerificationOutcome;
import java.util.List;
import org.junit.jupiter.api.Test;
import sharedkernel.application.paging.Page;
import sharedkernel.application.paging.PageRequest;

class ListAuthenticationAttemptsQueryHandlerTest {

    private final AuthenticationAttemptReadModel attempts = mock(AuthenticationAttemptReadModel.class);
    private final ListAuthenticationAttemptsQueryHandler handler =
        new ListAuthenticationAttemptsQueryHandler(attempts);

    @Test
    void shouldMapAPageOfAttempts() {
        var request = PageRequest.of(2, 10);
        when(attempts.find(request)).thenReturn(new Page<>(
            List.of(new AuthenticationAttempt(NOW, VerificationOutcome.LIVENESS_FAILED)), 2, 10, 11));

        var result = handler.handle(new ListAuthenticationAttemptsQuery(request));

        assertThat(result.value().pageNumber()).isEqualTo(2);
        assertThat(result.value().pageSize()).isEqualTo(10);
        assertThat(result.value().totalCount()).isEqualTo(11);
        assertThat(result.value().items()).singleElement().satisfies(attempt -> {
            assertThat(attempt.occurredOn()).isEqualTo(NOW);
            assertThat(attempt.outcome()).isEqualTo("LIVENESS_FAILED");
        });
    }
}
