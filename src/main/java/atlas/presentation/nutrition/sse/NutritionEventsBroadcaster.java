package atlas.presentation.nutrition.sse;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.nutrition.events.IntakeCorrectedEvent;
import atlas.domain.nutrition.events.IntakeDeletedEvent;
import atlas.domain.nutrition.events.IntakeRecordedEvent;
import atlas.domain.nutrition.events.PlanAdjustedEvent;
import atlas.domain.nutrition.events.PlanArchivedEvent;
import atlas.domain.nutrition.events.PlanDefinedEvent;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.util.Map;

public final class NutritionEventsBroadcaster {

    private NutritionEventsBroadcaster() {}

    public static void subscribeAll(SimpleDomainEventPublisher events, SseHub hub) {
        events.subscribe(PlanDefinedEvent.class,
            event -> broadcast(hub, "planDefined", "planId", event.planId().toString()));

        events.subscribe(PlanAdjustedEvent.class,
            event -> broadcast(hub, "planAdjusted", "planId", event.planId().toString()));

        events.subscribe(PlanArchivedEvent.class,
            event -> broadcast(hub, "planArchived", "planId", event.planId().toString()));

        events.subscribe(IntakeRecordedEvent.class,
            event -> broadcast(hub, "intakeRecorded", "intakeId", event.intakeId().toString()));

        events.subscribe(IntakeCorrectedEvent.class,
            event -> broadcast(hub, "intakeCorrected", "intakeId", event.intakeId().toString()));

        events.subscribe(IntakeDeletedEvent.class,
            event -> broadcast(hub, "intakeDeleted", "intakeId", event.intakeId().toString()));
    }

    private static void broadcast(SseHub hub, String name, String field, String id) {
        hub.broadcast(SseEvent.named(name, Json.write(Map.of(field, id))));
    }
}
