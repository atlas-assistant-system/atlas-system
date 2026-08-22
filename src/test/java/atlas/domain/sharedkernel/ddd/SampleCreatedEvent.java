package atlas.domain.sharedkernel.ddd;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

record SampleCreatedEvent(SampleId id, Instant occurredOn) implements DomainEvent {}
