package atlas.application.sharedkernel.outbox;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

record NoteRenamed(long id, Instant occurredOn) implements DomainEvent {}
