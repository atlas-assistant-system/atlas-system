package atlas.presentation.presence.sse;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.presence.events.SessionClosedEvent;
import atlas.domain.presence.events.SessionExpiredEvent;
import atlas.domain.presence.events.SessionOpenedEvent;
import atlas.domain.presence.events.SessionRefreshedEvent;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.util.Map;

public final class PresenceEventsBroadcaster {

    private PresenceEventsBroadcaster() {}

    public static void subscribeAll(SimpleDomainEventPublisher events, SseHub hub) {
        events.subscribe(SessionOpenedEvent.class, event -> broadcast(
            hub, "sessionOpened", event.sessionId().toString(), event.occurredOn().toString()));
        events.subscribe(SessionRefreshedEvent.class, event -> broadcast(
            hub, "sessionRefreshed", event.sessionId().toString(), event.occurredOn().toString()));
        events.subscribe(SessionExpiredEvent.class, event -> broadcast(
            hub, "sessionExpired", event.sessionId().toString(), event.occurredOn().toString()));
        events.subscribe(SessionClosedEvent.class, event -> broadcast(
            hub, "sessionClosed", event.sessionId().toString(), event.occurredOn().toString()));
    }

    private static void broadcast(SseHub hub, String name, String sessionId, String occurredOn) {
        hub.broadcast(SseEvent.named(name, Json.write(Map.of("sessionId", sessionId, "occurredOn", occurredOn))));
    }
}
