package atlas.presentation.presence.sse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.presence.ports.LivenessChallengeRepository;
import atlas.application.sharedkernel.cqrs.CommandBus;
import atlas.domain.sharedkernel.results.Result;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PresencePollerTest {

    @Test
    void shouldExpireSessionsAndChallenges() {
        var now = Instant.parse("2026-08-19T10:00:00Z");
        var commands = mock(CommandBus.class);
        var challenges = mock(LivenessChallengeRepository.class);
        when(commands.dispatch(any())).thenReturn(Result.success(1));
        var poller = new PresencePoller(commands, challenges, new SseHub(), Clock.fixed(now, ZoneOffset.UTC));

        poller.poll();

        verify(commands).dispatch(any());
        verify(challenges).removeExpired(now);
    }
}
