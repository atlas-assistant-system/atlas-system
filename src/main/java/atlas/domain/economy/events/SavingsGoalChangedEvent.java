package atlas.domain.economy.events;

import atlas.domain.economy.SavingsGoalId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record SavingsGoalChangedEvent(SavingsGoalId goalId, Instant occurredOn) implements DomainEvent {}
