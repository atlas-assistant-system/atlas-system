package sharedkernel.application.events;

import sharedkernel.domain.events.DomainEvent;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
