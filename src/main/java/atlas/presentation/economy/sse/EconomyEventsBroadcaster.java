package atlas.presentation.economy.sse;

import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.economy.events.BudgetDefinedEvent;
import atlas.domain.economy.events.BudgetLimitChangedEvent;
import atlas.domain.economy.events.BudgetRemovedEvent;
import atlas.domain.economy.events.MovementCorrectedEvent;
import atlas.domain.economy.events.MovementDeletedEvent;
import atlas.domain.economy.events.MovementRecategorizedEvent;
import atlas.domain.economy.events.MovementRecordedEvent;
import atlas.domain.economy.events.SavingsGoalAbandonedEvent;
import atlas.domain.economy.events.SavingsGoalChangedEvent;
import atlas.domain.economy.events.SavingsGoalSetEvent;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.util.Map;

public final class EconomyEventsBroadcaster {

    private EconomyEventsBroadcaster() {}

    public static void subscribeAll(SimpleDomainEventPublisher events, SseHub hub) {
        events.subscribe(MovementRecordedEvent.class,
            event -> broadcast(hub, "movementRecorded", "movementId", event.movementId().toString()));

        events.subscribe(MovementCorrectedEvent.class,
            event -> broadcast(hub, "movementCorrected", "movementId", event.movementId().toString()));

        events.subscribe(MovementRecategorizedEvent.class,
            event -> broadcast(hub, "movementRecategorized", "movementId", event.movementId().toString()));

        events.subscribe(MovementDeletedEvent.class,
            event -> broadcast(hub, "movementDeleted", "movementId", event.movementId().toString()));

        events.subscribe(BudgetDefinedEvent.class,
            event -> broadcast(hub, "budgetDefined", "budgetId", event.budgetId().toString()));

        events.subscribe(BudgetLimitChangedEvent.class,
            event -> broadcast(hub, "budgetLimitChanged", "budgetId", event.budgetId().toString()));

        events.subscribe(BudgetRemovedEvent.class,
            event -> broadcast(hub, "budgetRemoved", "budgetId", event.budgetId().toString()));

        events.subscribe(SavingsGoalSetEvent.class,
            event -> broadcast(hub, "savingsGoalSet", "goalId", event.goalId().toString()));

        events.subscribe(SavingsGoalChangedEvent.class,
            event -> broadcast(hub, "savingsGoalChanged", "goalId", event.goalId().toString()));

        events.subscribe(SavingsGoalAbandonedEvent.class,
            event -> broadcast(hub, "savingsGoalAbandoned", "goalId", event.goalId().toString()));
    }

    private static void broadcast(SseHub hub, String name, String field, String id) {
        hub.broadcast(SseEvent.named(name, Json.write(Map.of(field, id))));
    }
}
