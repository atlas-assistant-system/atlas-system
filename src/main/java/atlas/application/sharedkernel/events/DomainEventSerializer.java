package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.events.DomainEvent;

public interface DomainEventSerializer {

    String typeOf(DomainEvent event);

    String serialize(DomainEvent event);

    DomainEvent deserialize(String type, String payload);
}
