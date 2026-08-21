package atlas.domain.home.events;

import atlas.domain.home.HomeProfileId;
import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record HomeConfiguredEvent(HomeProfileId profileId, Instant occurredOn) implements DomainEvent {}
