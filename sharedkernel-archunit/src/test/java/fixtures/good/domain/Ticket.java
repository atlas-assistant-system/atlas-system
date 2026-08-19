package fixtures.good.domain;

import java.time.Instant;
import sharedkernel.domain.ddd.AggregateRoot;

public final class Ticket extends AggregateRoot<Long> {

    public Ticket(long id) {
        super(id);
    }

    public void open(Instant now) {
        registerEvent(new TicketOpened(id(), now));
    }
}
