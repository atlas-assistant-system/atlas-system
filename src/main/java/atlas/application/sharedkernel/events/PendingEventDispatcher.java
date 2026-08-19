package atlas.application.sharedkernel.events;

import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import java.util.List;

public final class PendingEventDispatcher {

    private final DomainEventPublisher publisher;

    public PendingEventDispatcher(DomainEventPublisher publisher) {
        this.publisher = ObjectGuard.notNull(publisher, "publisher");
    }

    public void dispatch(List<? extends AggregateRoot<?>> aggregates) {
        ObjectGuard.notNull(aggregates, "aggregates");

        for (var aggregate : aggregates) {
            var events = List.copyOf(aggregate.pendingEvents());
            aggregate.clearEvents();

            for (var event : events) {
                publisher.publish(event);
            }
        }
    }
}
