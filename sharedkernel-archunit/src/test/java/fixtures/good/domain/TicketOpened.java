package fixtures.good.domain;

import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public record TicketOpened(long ticketId, Instant occurredOn) implements DomainEvent {}
