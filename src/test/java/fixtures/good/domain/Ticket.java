package fixtures.good.domain;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import java.time.Instant;

public final class Ticket extends AggregateRoot<Long> {

    public Ticket(long id) {
        super(id);
    }

    public void open(Instant now) {
        registerEvent(new TicketOpened(id(), now));
    }
}
