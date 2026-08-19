package fixtures.good.domain;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

public record TicketOpened(long ticketId, Instant occurredOn) implements DomainEvent {}
