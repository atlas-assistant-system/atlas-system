package atlas.domain.sharedkernel.ddd;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.time.Instant;

record UntimedEvent() implements DomainEvent {

    @Override
    public Instant occurredOn() {
        return null;
    }
}
