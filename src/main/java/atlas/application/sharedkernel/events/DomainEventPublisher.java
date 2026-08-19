package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.events.DomainEvent;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
