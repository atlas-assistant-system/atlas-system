package atlas.infrastructure.sharedkernel.persistence;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

record NoteRenamed(NoteId id, Instant occurredOn) implements DomainEvent {}
