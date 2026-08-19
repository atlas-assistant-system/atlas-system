package sharedkernel.domain.ddd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sharedkernel.domain.events.DomainEvent;
import sharedkernel.domain.guards.ObjectGuard;

public abstract class AggregateRoot<TId> extends Entity<TId> {

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    protected AggregateRoot(TId id) {
        super(id);
    }

    protected void registerEvent(DomainEvent event) {
        ObjectGuard.notNull(event, "event");
        ObjectGuard.notNull(event.occurredOn(), "event.occurredOn");

        pendingEvents.add(event);
    }

    public List<DomainEvent> pendingEvents() {
        return Collections.unmodifiableList(pendingEvents);
    }

    public void clearEvents() {
        pendingEvents.clear();
    }
}
