package atlas.domain.economy.events;

import atlas.domain.economy.BudgetId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record BudgetLimitChangedEvent(BudgetId budgetId, Instant occurredOn) implements DomainEvent {}
