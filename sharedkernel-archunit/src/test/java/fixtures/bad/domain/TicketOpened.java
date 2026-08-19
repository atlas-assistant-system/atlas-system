package fixtures.bad.domain;

import java.time.Instant;
import sharedkernel.domain.events.DomainEvent;

public final class TicketOpened implements DomainEvent {

    private final Instant occurredOn;

    public TicketOpened(Instant occurredOn) {
        this.occurredOn = occurredOn;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }
}
