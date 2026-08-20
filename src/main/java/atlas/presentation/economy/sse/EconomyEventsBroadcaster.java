package atlas.presentation.economy.sse;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.economy.MovementId;
import atlas.domain.economy.events.MovementCorrectedEvent;
import atlas.domain.economy.events.MovementDeletedEvent;
import atlas.domain.economy.events.MovementRecategorizedEvent;
import atlas.domain.economy.events.MovementRecordedEvent;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.util.Map;

public final class EconomyEventsBroadcaster {

    private EconomyEventsBroadcaster() {}

    public static void subscribeAll(SimpleDomainEventPublisher events, SseHub hub) {
        events.subscribe(MovementRecordedEvent.class,
            event -> broadcast(hub, "movementRecorded", event.movementId()));

        events.subscribe(MovementCorrectedEvent.class,
            event -> broadcast(hub, "movementCorrected", event.movementId()));

        events.subscribe(MovementRecategorizedEvent.class,
            event -> broadcast(hub, "movementRecategorized", event.movementId()));

        events.subscribe(MovementDeletedEvent.class,
            event -> broadcast(hub, "movementDeleted", event.movementId()));
    }

    private static void broadcast(SseHub hub, String name, MovementId movementId) {
        hub.broadcast(SseEvent.named(name, Json.write(Map.of("movementId", movementId.toString()))));
    }
}
