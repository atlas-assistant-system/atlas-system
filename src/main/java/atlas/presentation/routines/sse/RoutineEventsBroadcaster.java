package atlas.presentation.routines.sse;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.routines.events.DayClearedEvent;
import atlas.domain.routines.events.ProgressLoggedEvent;
import atlas.domain.routines.events.RoutineArchivedEvent;
import atlas.domain.routines.events.RoutineDefinedEvent;
import atlas.domain.routines.events.RoutineDeletedEvent;
import atlas.domain.routines.events.RoutineDetailsChangedEvent;
import atlas.domain.routines.events.RoutineScheduleChangedEvent;
import atlas.domain.routines.events.RoutineUnarchivedEvent;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RoutineEventsBroadcaster {

    private RoutineEventsBroadcaster() {}

    public static void subscribeAll(SimpleDomainEventPublisher events, SseHub hub) {
        events.subscribe(RoutineDefinedEvent.class, event -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("routineId", event.routineId().toString());
            data.put("name", event.name().value());
            hub.broadcast(SseEvent.named("routineDefined", Json.write(data)));
        });

        events.subscribe(RoutineScheduleChangedEvent.class, event -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("routineId", event.routineId().toString());
            data.put("period", event.schedule().period().name());
            hub.broadcast(SseEvent.named("routineScheduleChanged", Json.write(data)));
        });

        events.subscribe(RoutineDetailsChangedEvent.class, event -> hub.broadcast(SseEvent.named(
            "routineDetailsChanged", Json.write(Map.of("routineId", event.routineId().toString())))));

        events.subscribe(RoutineArchivedEvent.class, event -> hub.broadcast(SseEvent.named(
            "routineArchived", Json.write(Map.of("routineId", event.routineId().toString())))));

        events.subscribe(RoutineUnarchivedEvent.class, event -> hub.broadcast(SseEvent.named(
            "routineUnarchived", Json.write(Map.of("routineId", event.routineId().toString())))));

        events.subscribe(RoutineDeletedEvent.class, event -> hub.broadcast(SseEvent.named(
            "routineDeleted", Json.write(Map.of("routineId", event.routineId().toString())))));

        events.subscribe(ProgressLoggedEvent.class, event -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("routineId", event.routineId().toString());
            data.put("day", event.day().toString());
            data.put("amount", event.amount().toPlainString());
            hub.broadcast(SseEvent.named("progressLogged", Json.write(data)));
        });

        events.subscribe(DayClearedEvent.class, event -> {
            var data = new LinkedHashMap<String, Object>();
            data.put("routineId", event.routineId().toString());
            data.put("day", event.day().toString());
            hub.broadcast(SseEvent.named("dayCleared", Json.write(data)));
        });
    }
}
