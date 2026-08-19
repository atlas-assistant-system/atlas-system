package fixtures.bad.domain;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

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
