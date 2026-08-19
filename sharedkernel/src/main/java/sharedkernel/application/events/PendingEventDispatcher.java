package sharedkernel.application.events;

import java.util.List;
import sharedkernel.domain.ddd.AggregateRoot;
import sharedkernel.domain.guards.ObjectGuard;

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
