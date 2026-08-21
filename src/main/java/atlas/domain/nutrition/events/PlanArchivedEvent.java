package atlas.domain.nutrition.events;

import atlas.domain.nutrition.PlanId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record PlanArchivedEvent(PlanId planId, Instant occurredOn) implements DomainEvent {}
