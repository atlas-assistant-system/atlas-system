package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

record SomethingElseHappened(Instant occurredOn) implements DomainEvent {}
