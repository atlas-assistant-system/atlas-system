package atlas.application.sharedkernel.unitofwork;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

record SampleChanged(SampleId id, Instant occurredOn) implements DomainEvent {}
