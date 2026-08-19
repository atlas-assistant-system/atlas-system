package sharedkernel.application.events;

import sharedkernel.domain.events.DomainEvent;

public interface DomainEventHandler<E extends DomainEvent> {

    void handle(E event);
}
