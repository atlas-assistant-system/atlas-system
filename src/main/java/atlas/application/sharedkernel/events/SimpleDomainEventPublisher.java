package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.events.DomainEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SimpleDomainEventPublisher implements DomainEventPublisher {

    private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new HashMap<>();

    public <E extends DomainEvent> void subscribe(Class<E> eventType, DomainEventHandler<E> handler) {
        handlers.computeIfAbsent(eventType, key -> new ArrayList<>()).add(handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        for (var handler : handlers.getOrDefault(event.getClass(), List.of())) {
            ((DomainEventHandler<DomainEvent>) handler).handle(event);
        }
    }
}
