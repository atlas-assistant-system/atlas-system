package sharedkernel.application.events;

import sharedkernel.domain.events.DomainEvent;

public interface DomainEventSerializer {

    String typeOf(DomainEvent event);

    String serialize(DomainEvent event);

    DomainEvent deserialize(String type, String payload);
}
