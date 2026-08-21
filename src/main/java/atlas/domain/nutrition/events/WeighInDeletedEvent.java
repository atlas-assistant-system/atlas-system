package atlas.domain.nutrition.events;

import atlas.domain.nutrition.WeighInId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record WeighInDeletedEvent(WeighInId weighInId, Instant occurredOn) implements DomainEvent {}
