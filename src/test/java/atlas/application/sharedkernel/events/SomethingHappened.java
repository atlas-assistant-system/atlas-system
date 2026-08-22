package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

record SomethingHappened(String detail, Instant occurredOn) implements DomainEvent {}
