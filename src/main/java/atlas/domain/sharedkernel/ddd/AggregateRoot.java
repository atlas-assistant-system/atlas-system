package atlas.domain.sharedkernel.ddd;

import atlas.domain.sharedkernel.events.DomainEvent;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
