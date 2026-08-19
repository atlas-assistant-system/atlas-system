package atlas.domain.sharedkernel.events;

import java.time.Instant;

public interface DomainEvent {

    Instant occurredOn();
}
