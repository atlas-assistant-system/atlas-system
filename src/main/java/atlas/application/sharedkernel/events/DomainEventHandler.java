package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.events.DomainEvent;

public interface DomainEventHandler<E extends DomainEvent> {

    void handle(E event);
}
