package atlas.presentation.presence.sse;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.presence.SessionId;
import atlas.domain.presence.events.SessionClosedEvent;
import atlas.domain.presence.events.SessionExpiredEvent;
import atlas.domain.presence.events.SessionOpenedEvent;
import atlas.domain.presence.events.SessionRefreshedEvent;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import sharedkernel.application.events.SimpleDomainEventPublisher;
import sharedkernel.presentation.sse.SseHub;

class PresenceEventsBroadcasterTest {

    private static final Instant NOW = Instant.parse("2026-08-19T10:00:00Z");

    private final SimpleDomainEventPublisher events = new SimpleDomainEventPublisher();
    private final SseHub hub = new SseHub();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Test
    void shouldBroadcastEverySessionLifecycleEvent() {
        hub.register(output);
        PresenceEventsBroadcaster.subscribeAll(events, hub);
        var id = SessionId.of(7);

        events.publish(new SessionOpenedEvent(id, NOW));
        events.publish(new SessionRefreshedEvent(id, NOW.plusSeconds(1)));
        events.publish(new SessionExpiredEvent(id, NOW.plusSeconds(2)));
        events.publish(new SessionClosedEvent(id, NOW.plusSeconds(3)));

        var wire = output.toString(StandardCharsets.UTF_8);
        assertThat(wire)
            .contains("event: sessionOpened")
            .contains("event: sessionRefreshed")
            .contains("event: sessionExpired")
            .contains("event: sessionClosed")
            .contains("\"sessionId\":\"S00000007\"")
            .contains("\"occurredOn\":\"2026-08-19T10:00:00Z\"");
    }
}
